# Auto Service CRM Backend

MVP backend для информационной системы автосервиса.

Проект реализован в рамках практики и покрывает основной бизнес-сценарий работы автосервиса: ведение клиентов, автомобилей, мастеров, услуг, запчастей, заказов и оплат.

## Стек

- Kotlin
- Ktor
- PostgreSQL
- Exposed
- Flyway
- HikariCP
- Swagger / OpenAPI

## Реализованный функционал

### Справочники
- клиенты
- автомобили
- мастера
- услуги
- запчасти

### Заказы
- создание заказа
- получение списка заказов
- получение заказа по id
- обновление заказа
- удаление заказа
- фильтрация заказов по `clientId` и `status`
- получение полной карточки заказа

### Состав заказа
- добавление услуг в заказ
- обновление услуг в заказе
- удаление услуг из заказа
- добавление запчастей в заказ
- обновление запчастей в заказе
- удаление запчастей из заказа

### Оплаты
- создание оплаты по заказу
- получение списка оплат по заказу

### Дополнительно
- автоматический пересчет общей суммы заказа
- контроль остатков запчастей при работе с заказом
- миграции БД через Flyway
- документация API через Swagger
- JSON-ответы при ошибках

## Структура проекта

```text
src/main/kotlin/com/example/
├─ common/
│  ├─ enums/
│  ├─ ErrorResponse.kt
│  ├─ BadRequestException.kt
│  ├─ NotFoundException.kt
│  └─ ConflictException.kt
├─ database/
│  ├─ DatabaseFactory.kt
│  ├─ FlywayFactory.kt
│  └─ tables/
├─ features/
│  ├─ clients/
│  ├─ cars/
│  ├─ masters/
│  ├─ services/
│  ├─ parts/
│  └─ orders/
├─ plugins/
└─ Application.kt
```

## Конфигурация

Настройки БД находятся в:

```text
src/main/resources/application.yaml
```

### Пример `application.yaml`

```yaml
ktor:
  application:
    modules:
      - com.example.ApplicationKt.module
  deployment:
    port: 8080

database:
  driver: "org.postgresql.Driver"
  url: "jdbc:postgresql://localhost:5432/autoservice_db"
  user: "postgres"
  password: "your_password"
```

### P.S. Коммиты в master, потому что разработка велась 1 человеком. Автор: Родион Пиперов
