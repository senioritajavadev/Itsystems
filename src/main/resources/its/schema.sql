-- Схема IT-S. Выполняется при каждом запуске приложения, поэтому все команды идемпотентны.
CREATE SCHEMA IF NOT EXISTS its;

CREATE TABLE IF NOT EXISTS its.clients
(
    id         serial PRIMARY KEY,
    full_name  text        NOT NULL,
    phone      text        NOT NULL UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- Заказ на ремонт: id — это «Номер заказа» в карточке на Главной
CREATE TABLE IF NOT EXISTS its.orders
(
    id            serial PRIMARY KEY,
    received_date date        NOT NULL DEFAULT CURRENT_DATE,
    issued_date   date,
    device_brand  text        NOT NULL DEFAULT '',
    device_model  text        NOT NULL DEFAULT '',
    client_id     integer     NOT NULL REFERENCES its.clients (id),
    defect        text        NOT NULL DEFAULT '',
    after_what    text        NOT NULL DEFAULT '',
    notes         text        NOT NULL DEFAULT '',
    status        varchar(20) NOT NULL DEFAULT 'IN_PROGRESS'
        CHECK (status IN ('IN_PROGRESS', 'DONE')),
    price         numeric(12, 2) CHECK (price >= 0)
);

-- Для баз, созданных до появления цены в заказе
ALTER TABLE its.orders ADD COLUMN IF NOT EXISTS price numeric(12, 2) CHECK (price >= 0);

CREATE INDEX IF NOT EXISTS orders_status_idx ON its.orders (status);

CREATE TABLE IF NOT EXISTS its.services
(
    id    serial PRIMARY KEY,
    name  text           NOT NULL UNIQUE,
    price numeric(12, 2) NOT NULL CHECK (price >= 0)
);

CREATE TABLE IF NOT EXISTS its.sales
(
    id        serial PRIMARY KEY,
    sale_date date           NOT NULL DEFAULT CURRENT_DATE,
    title     text           NOT NULL,
    quantity  integer        NOT NULL DEFAULT 1 CHECK (quantity > 0),
    price     numeric(12, 2) NOT NULL CHECK (price >= 0),
    client_id integer        REFERENCES its.clients (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS sales_date_idx ON its.sales (sale_date);

CREATE TABLE IF NOT EXISTS its.expenses
(
    id           serial PRIMARY KEY,
    expense_date date           NOT NULL DEFAULT CURRENT_DATE,
    title        text           NOT NULL,
    amount       numeric(12, 2) NOT NULL CHECK (amount >= 0)
);

CREATE INDEX IF NOT EXISTS expenses_date_idx ON its.expenses (expense_date);
