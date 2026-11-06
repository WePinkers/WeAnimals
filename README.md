# WeAnimals

Aplicativo Android da plataforma WeAnimals, desenvolvido como projeto de TCC em Engenharia de Software.

O WeAnimals conecta usuários, adotantes, ONGs e abrigos para apoiar a proteção animal, facilitar denúncias, promover adoções responsáveis e acompanhar os atendimentos.

## Integrantes do Grupo

| RA | Nome completo |
| --- | --- |
| 823115360 | Alissa Novais Wenceslau Ferreira |
| 823115679 | Carolina Ornelas de Almeida |
| 823113068 | Mayara Borges de Carvalho |
| 823119535 | Támer Issa Ubaid |
| 824152542 | Felipe Menegazzi Garofalo |

## Funcionalidades

- Cadastro e autenticação de usuários
- Login com e-mail e Google
- Perfil do usuário e preferências de adoção
- Denúncias de animais em situação de risco
- Triagem e classificação de denúncias por urgência
- Acompanhamento do status de uma denúncia
- Chat entre usuário e equipe responsável pelo atendimento
- Mapa com ocorrências próximas e localização das denúncias
- Adoção responsável de animais
- Questionário de compatibilidade entre adotante e animal
- Candidaturas de adoção e acompanhamento do processo
- Conversas entre adotante e ONG ou abrigo
- Check-ins pós-adoção de 30, 90 e 180 dias
- Comunidade com publicações, campanhas, curtidas e comentários
- Animais perdidos e encontrados
- Ouvidoria e suporte aos usuários
- Notificações relacionadas aos casos e interações

## Tecnologias utilizadas

### Aplicativo

- Kotlin
- Android SDK
- Gradle com Kotlin DSL
- Android Gradle Plugin
- Java 11
- View Binding
- Layouts XML e componentes tradicionais do Android
- Material Components
- ConstraintLayout
- RecyclerView
- ViewPager2
- Kotlin Coroutines
- AndroidX Lifecycle

### Firebase

- Firebase Authentication
- Firebase Firestore
- Firebase Analytics
- Regras de segurança do Firestore
- Índices do Firestore

### Mapas, localização e imagens

- Google Maps SDK for Android
- MapLibre Android SDK
- Google Play Services Location
- Google Sign-In
- Coil para carregamento de imagens
- AndroidX ExifInterface
- Android-Image-Cropper

> A interface do aplicativo utiliza Views, XML e View Binding. O projeto não utiliza Jetpack Compose.

## Estrutura do projeto

```text
app/src/main/java/com/example/weanimals/
├── core/
│   ├── base/
│   ├── di/
│   ├── formatting/
│   ├── location/
│   ├── navigation/
│   ├── notification/
│   ├── session/
│   ├── theme/
│   └── utils/
└── user/
    ├── adoption/
    ├── community/
    ├── entry/
    ├── home/
    ├── lostandfound/
    ├── map/
    ├── ombudsman/
    ├── profile/
    └── reporting/
```

As funcionalidades são organizadas por contexto. Quando necessário, cada contexto possui camadas de domínio, interactor, presenter, repository e presentation.

## Requisitos

- Android Studio
- JDK 11 ou o JDK incluído no Android Studio
- Android SDK com API 37
- Um dispositivo Android ou emulador com Android 7.0 (API 24) ou superior
- Acesso ao projeto Firebase do WeAnimals

## Configuração local

1. Clone o repositório:

   ```bash
   git clone https://github.com/WePinkers/WeAnimals.git
   cd WeAnimals
   ```

2. Abra a pasta no Android Studio e aguarde a sincronização do Gradle.

3. Configure o Firebase:

   - Adicione o arquivo `google-services.json` em `app/`.
   - Confirme que o aplicativo Android está cadastrado no projeto Firebase.
   - Ative Authentication, Firestore e os demais serviços utilizados pelo aplicativo.

4. Configure a chave de mapas no `local.properties`:

   ```properties
   MAPS_API_KEY=SUA_CHAVE_DO_GOOGLE_MAPS
   ```

   O arquivo `local.properties` não deve ser commitado. Restrinja a chave no Google Cloud Console para o aplicativo Android sempre que possível.

5. Publique as regras e índices do Firestore somente quando necessário:

   ```bash
   firebase use weanimals-642a9
   firebase deploy --only firestore:rules,firestore:indexes
   ```

## Executar o aplicativo

No Android Studio, selecione o módulo `app`, escolha um dispositivo e clique em **Run**.

Também é possível executar pelo terminal no Windows:

```powershell
.\gradlew.bat installDebug
```

## Build e testes

Gerar a APK de debug:

```powershell
.\gradlew.bat assembleDebug
```

A APK será gerada em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Executar os testes unitários:

```powershell
.\gradlew.bat testDebugUnitTest
```

Executar o lint:

```powershell
.\gradlew.bat lintDebug
```

Executar os testes instrumentados com um emulador ou dispositivo conectado:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Para limpar os artefatos de build:

```powershell
.\gradlew.bat clean
```

## Firebase e sincronização

O aplicativo utiliza o Firestore como fonte de dados para manter as informações sincronizadas entre usuários, ONGs e abrigos. As regras de segurança definem quais dados cada tipo de conta pode consultar ou alterar.

As principais coleções utilizadas incluem:

- `user_profiles`
- `reports`
- `community_posts`
- `community_campaigns`
- `animais`
- `adoption_applications`
- `adoption_conversations`
- `organization_verification_requests`
- `organizations`

Antes de testar uma funcionalidade que depende de dados reais, confira se o Firebase está configurado, se o usuário está autenticado e se as regras e índices publicados correspondem à versão atual do aplicativo.

## Geração de uma versão para avaliação

Para gerar uma APK de debug para demonstração:

```powershell
.\gradlew.bat assembleDebug
```

Depois, envie o arquivo `app-debug.apk` ao avaliador ou anexe-o a uma versão do GitHub Releases.

Para gerar uma versão de release, é necessário configurar uma chave de assinatura Android. As credenciais de assinatura não devem ser armazenadas no repositório.

## Projeto acadêmico

O WeAnimals é um projeto de TCC desenvolvido em equipe com o objetivo de aplicar conceitos de Engenharia de Software em uma solução voltada à proteção e ao bem-estar animal.

## Licença

Projeto acadêmico de uso educacional. Consulte os responsáveis pelo projeto antes de redistribuir o código ou os materiais da aplicação.
