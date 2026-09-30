# triAI-api-java

API do triAI. Recebe os dados do paciente como um Bundle FHIR R4 e devolve a classificação de risco (Protocolo de Manchester) como um `RiskAssessment`. Tudo o que o hospital envia e recebe é FHIR; o serviço de ML (`triAI-ml`) é interno.

## Endpoints FHIR

Base: `/api/fhir`. Formato: `application/fhir+json` (`application/json` também é aceito). Exceto `metadata`, todas as chamadas exigem `Authorization: Bearer <token>`.

| Método e caminho | O que faz |
|---|---|
| `GET /metadata` | `CapabilityStatement` do servidor (público) |
| `POST /$triage` | Recebe um `Bundle` (ou `Parameters` com o Bundle) e devolve um `RiskAssessment` |
| `GET /RiskAssessment/{id}` | Lê uma classificação, com as alterações feitas pelo profissional |
| `POST /RiskAssessment/{id}/$amend` | Registra a cor final do profissional (`Parameters`) e devolve o `RiskAssessment` atualizado |
| `GET /ValueSet/chief-complaints/$expand` | Lista de palavras-chave de queixa, com designação em `pt-BR`. Os itens internos do modelo vêm com `abstract: true` e não devem ser oferecidos ao usuário |
| `GET /CodeSystem/{id}` | `chief-complaints` e `manchester-triage` |
| `GET /ConceptMap/chief-complaints` | Mapeamento de SNOMED CT e CID-10 para a nossa lista |
| `GET /OperationDefinition/{id}` | `triage` e `amend` |

Erros voltam como `OperationOutcome` (422 para dados inválidos, 400 para JSON malformado, 404, 401 e 500).

## Swagger

A tela interativa fica em `/swagger-ui.html` (em produção, `https://triai-api.onrender.com/swagger-ui.html`) e a especificação OpenAPI em `/v3/api-docs`. Ambas são públicas. Para testar as chamadas:

1. Em `POST /api/login`, informe e-mail e senha e copie o valor de `data.token` da resposta.
2. Clique em **Authorize** (canto superior direito) e cole o token.
3. Em `POST /api/fhir/$triage`, o corpo já vem preenchido com um Bundle de exemplo: basta clicar em **Execute**. O corpo é uma caixa de texto livre, porque o Bundle FHIR não tem um esquema fixo no OpenAPI.
4. Use o `id` da resposta em `GET /api/fhir/RiskAssessment/{id}` e em `POST /api/fhir/RiskAssessment/{id}/$amend`.

O Swagger não expõe dados: as chamadas continuam exigindo o token. Para uma descrição no padrão FHIR, veja `GET /api/fhir/metadata`.

## O que o servidor lê do Bundle

- **Patient**: `gender` e `birthDate`.
- **Observation** com código LOINC ou SNOMED CT (o sistema do código não é verificado):

| Sinal | Códigos aceitos |
|---|---|
| Frequência cardíaca | `8867-4`, `8889-8`, `364075005`, `78564009` |
| Frequência respiratória | `9279-1`, `86290005` |
| Saturação de O2 | `59408-5`, `2708-6`, `20564-1`, `431314004` |
| Temperatura | `8310-5`, `8331-1`, `8328-7`, `386725007` |
| Pressão sistólica e diastólica | `8480-6` e `8462-4`, avulsas ou como `component` de `85354-9`, `55284-4` ou `75367002` (SNOMED `271649006` e `271650006`) |
| Dor | `72514-3`, `38208-5` |
| Queixa principal | `10154-3` (texto) |

- **Unidades** (UCUM): temperatura em Cel, °F ou K vira °C; pressão em kPa vira mmHg; saturação como fração vira %. Sem unidade, valores de temperatura acima de 50 são tratados como °F. Unidade desconhecida ignora o valor e gera aviso.
- Entre várias Observations do mesmo sinal vale a de `effective[x]` mais recente. `entered-in-error` e `cancelled` são ignoradas.
- **Encounter**: `reasonCode` (código do vocabulário do triAI, SNOMED CT ou CID-10 mapeados pelo ConceptMap, ou texto), `reasonReference` para `Condition` ou `Observation` dentro do Bundle, e a chegada de ambulância pela extensão `https://triai.local/fhir/StructureDefinition/arrival-mode` (`valueCode: ambulance`) ou por um `hospitalization.admitSource` que cite ambulância ou helicóptero.

Se nenhum sinal vital for reconhecido, a API recusa o Bundle (422) e lista os códigos desconhecidos. Sinais faltantes, pouca cobertura e códigos não reconhecidos voltam como `note` do `RiskAssessment`.

## Resposta: RiskAssessment

- `status`: `final`, `preliminary` (modelo indisponível, vermelho aplicado por segurança) ou `amended` (há alteração do profissional).
- `prediction[0]`: `outcome` com a cor em `https://triai.local/fhir/CodeSystem/manchester-triage` (`red`, `orange`, `yellow`, `green`, `blue`) e `probabilityDecimal` com a confiança.
- Cada alteração vira mais uma `prediction` (cor final e `rationale` com o motivo); a última vale.
- `prediction[0]` carrega, em extensões, a **distribuição de probabilidade entre as cores**: uma extensão `https://triai.local/fhir/StructureDefinition/color-probability` por cor, com as sub-extensões `color` (Coding do Protocolo de Manchester) e `probability` (decimal). Quando a cor Vermelho foi definida pela regra de sinais vitais críticos, há também a extensão `.../safety-rule` (`valueBoolean: true`) e uma `note` explicando. Essas informações vêm só na resposta de `$triage`: não são gravadas no banco, então `GET /RiskAssessment/{id}` não as traz.
- `subject`, `encounter` e `basis` referenciam o que veio no Bundle.

## Configuração

Variáveis em `.env.example`. `TRIAGE_MODEL_URL` aponta para o serviço `triAI-ml`.

## Testes

```
./mvnw test -Dtest='Fhir*Test,UnitConverterTest,RiskAssessmentMapperTest,ChiefComplaintTerminologyServiceTest'
```

## Limitações conhecidas

- Autenticação por token Bearer próprio. SMART on FHIR não é suportado.
- A lista de códigos LOINC e SNOMED e o ConceptMap são uma versão inicial, sem revisão clínica.
- Apenas `reasonCode`, `reasonReference` e `10154-3` alimentam a queixa. Hospitais com outro modelo de dados precisam de mapeamento.
- Chegada de ambulância, queixa e sintomas vão ao modelo, mas não são gravados no banco. O sexo do `Patient` é lido, mas o modelo o ignora.

## Deploy no Render

O `Dockerfile` compila o projeto com Maven e roda o JAR em Java 17 (o processo usa cerca de 300 MB). O `render.yaml` descreve o serviço web no plano gratuito.

Pré-requisitos:

- **Um MySQL fora do Render** (o Render não oferece MySQL), acessível pela internet. O schema está em `database/schema.sql` e é idempotente: pode ser aplicado sobre um banco que já tem as tabelas. É preciso ter um usuário com `active = 1` e senha em bcrypt (`$2a$`, `$2b$` ou `$2y$`; os hashes gerados pelo Laravel funcionam).
- O serviço `triAI-ml` já no ar, para ter a URL dele.

Passos:

1. No Render, **New > Blueprint**, escolha este repositório e a branch `develop`.
2. Preencha as variáveis pedidas: `DB_HOST`, `DB_PORT`, `DB_DATABASE`, `DB_USERNAME`, `DB_PASSWORD`, `TRIAGE_MODEL_URL` (URL do ML, com `https://` e sem barra no fim) e `CORS_ALLOWED_ORIGINS` (URL do front).
3. Se o provedor do banco exigir TLS, ajuste `DB_SSL_MODE` (`REQUIRED`).
4. Planos gratuitos de MySQL limitam as conexões por usuário (5 no Clever Cloud). O Blueprint já usa `DB_POOL_SIZE=3`; com o padrão de 10, a API ocupa todas as conexões e nem você consegue mais conectar ao banco. Se o seu limite for diferente, ajuste `DB_POOL_SIZE` para um valor menor que ele.
5. Confira: `GET <url>/api/fhir/metadata` devolve o `CapabilityStatement`.

No plano gratuito o ML "dorme" após inatividade e a primeira chamada pode demorar. `TRIAGE_MODEL_TIMEOUT_SECONDS` (55 no Blueprint) cobre essa espera; acima do timeout a API responde com Vermelho preliminar.
