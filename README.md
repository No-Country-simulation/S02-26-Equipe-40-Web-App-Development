# 🇺🇸 Nexus US & ConversionFlow | Plataforma Full-Stack

![Capa do Projeto](https://github.com/user-attachments/assets/4fe83855-faa3-4de7-a09c-f5bc13d00284)

> **Sobre o projeto:** Uma solução completa de e-commerce e formação de empresas (LLCs) nos EUA, projetada para founders globais. Une uma interface premium (focada em conversão e UX) a um back-end robusto focado no rastreamento confiável de pagamentos via Stripe e integração com plataformas de anúncios (Google/Meta Ads) e CRM.

🔗 **[Acesse o Deploy da Aplicação Aqui](https://nnexus.framer.website/)**

## 🚀 Tecnologias Utilizadas

**Front-end:** ![React](https://img.shields.io/badge/react-%2320232a.svg?style=for-the-badge&logo=react&logoColor=%2361DAFB)
![Framer](https://img.shields.io/badge/Framer-black?style=for-the-badge&logo=framer&logoColor=blue)
![CSS3](https://img.shields.io/badge/css3-%231572B6.svg?style=for-the-badge&logo=css3&logoColor=white)

**Back-end:** ![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Postgres](https://img.shields.io/badge/postgres-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)
![Stripe](https://img.shields.io/badge/Stripe-626CD9?style=for-the-badge&logo=Stripe&logoColor=white)

---

## 🧱 Arquitetura Full-Stack
```mermaid
%%{init: {'theme': 'dark'}}%%
flowchart LR
    classDef box fill:#111827,stroke:#374151,color:#ffffff,stroke-width:1px;

    A[Usuário]:::box
    B[React / Framer]:::box
    C[Stripe Checkout]:::box
    D[Stripe Webhook]:::box
    E[ConversionFlow API<br>Spring Boot]:::box
    F[Conversões Server-side]:::box
    G[Meta Ads]:::box
    H[Google Ads]:::box
    I[Pipedrive CRM]:::box

    A --> B --> C --> D --> E --> F
    F --> G
    F --> H
    G --> I
    H --> I
```

---

## 💻 Front-end (Nexus US)

A interface atua como a vitrine do produto, adotando a estética *Dark Mode* com *Glassmorphism* para transmitir segurança e alta tecnologia, focando na usabilidade e na captura eficiente de leads.

### 🏗️ Engenharia e UI/UX
* **Componentização React:** Desenvolvimento de *Code Components* 100% customizados dentro do ecossistema Framer.
* **Estilização Híbrida:** Uso de CSS-in-JS combinado com `<style>` injetados para animações complexas (`@keyframes`) e responsividade avançada (`ResizeObserver`).
* **Performance Vetorial:** Todos os ícones e elementos gráficos (como a logo e animações de background) são SVGs matemáticos nativos, garantindo zero requisições HTTP extras e qualidade Retina/4K.
* **Integração Assíncrona:** Gerenciamento de estado (`useState`) e comunicação direta com a API via `Fetch API` para captura de leads e geração de checkouts dinâmicos.

### 📸 Telas Principais
1. **Landing Page:** *Hero section* impactante focada em conversão com rolagem fluida.
2. **Autenticação (`NexusAuthPage`):** Captura de leads dinâmica integrada à API.
3. **Pricing (`PricingCardsPalette`):** Layout interativo que envia o usuário diretamente ao checkout do Stripe.
4. **Dashboard:** Painel do usuário utilizando SVGs dinâmicos para exibir o status da LLC e integrações.

---

## ⚙️ Back-end (ConversionFlow)

O backend atua como a **fonte única de verdade** para conversões e decisões de negócio. O ConversionFlow é uma aplicação backend desenvolvida em Java com Spring Boot, responsável por processar pagamentos reais via Stripe e registrar conversões de forma confiável para plataformas de marketing e CRM.

O sistema utiliza rastreamento *server-side*, garantindo dados precisos mesmo em cenários de bloqueio de cookies, restrições de privacidade e limitações de tracking *client-side*.

### 🎯 Objetivos do Backend
* Registrar **apenas pagamentos aprovados** como conversões.
* Garantir mensuração correta para Google Ads e Meta Ads.
* Integrar dados de vendas com o Pipedrive (CRM).
* Suportar campanhas de tráfego pago com dados absolutos e escaláveis.
* Manter uma arquitetura clara e escalável.

### 🧠 Princípio Central
> *"Pagamentos confirmados definem conversões. Eventos de navegador são apenas complementares."*

### 🔄 Fluxo de Conversão
1. Usuário clica em um anúncio e realiza o pagamento via Stripe.
2. Stripe envia um webhook (`payment_intent.succeeded`).
3. Backend valida o pagamento e a conversão é registrada *server-side*.
4. Evento é enviado para as APIs do Google Ads e Meta Ads.
5. Lead/Venda é sincronizado com o Pipedrive.

### 🗄️ Banco de Dados
**PostgreSQL** é utilizado por oferecer:
* Alta confiabilidade transacional.
* Suporte nativo a JSON/JSONB (essencial para payloads de webhook do Stripe).
* Boa performance para aplicações orientadas a eventos.

### 📂 Estrutura do Projeto
```text
backend/conversionflow
├── src/main/java/com/nocountry/conversionflow
│   ├── controller   # Endpoints REST e webhooks
│   ├── service      # Regras de negócio
│   ├── client       # Integrações externas
│   ├── domain       # Entidades de domínio
│   ├── repository   # Persistência (JPA)
│   ├── config       # Configurações
│   └── exception    # Exceções customizadas
├── src/main/resources
│   ├── application.yml
├── pom.xml
└── README.md
```

---

## 👨‍💻 Equipe 40

A união de design estratégico, front-end performático e back-end à prova de falhas:

* **Cássia Nascimento** - Frontend Engineer & Product Designer - [LinkedIn](https://www.linkedin.com/in/cassia-irene/) | [GitHub](https://github.com/Cassia-Irene)
* **Leonardo Ferreira** - Frontend Engineer & Product Designer - [LinkedIn](https://www.linkedin.com/in/leonardoferrza) | [GitHub](https://github.com/leonardoferrza)
* **Tirso Alcazar** - Backend Developer - [LinkedIn](https://www.linkedin.com/in/tirsoalcazar/) | [GitHub](https://github.com/HandlezGZ)
* **Webster Spacacheri** - Backend Developer - [LinkedIn](https://www.linkedin.com/in/webster-spacacheri/) | [GitHub](https://github.com/Webster-TI)


---

## 🚀 Como Executar (Local)

**Backend:**
```bash
cd backend/conversionflow
./mvnw spring-boot:run
```


