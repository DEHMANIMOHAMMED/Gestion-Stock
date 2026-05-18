create table supplier_sla_settings (
    id bigserial primary key,
    organisation_id bigint not null,
    supplier_id bigint not null references suppliers(id) on delete cascade,
    target_lead_time_days integer not null,
    target_conformity_rate numeric(5,2) not null,
    target_on_time_rate numeric(5,2) not null,
    notes varchar(500),
    updated_at timestamp not null,
    unique (organisation_id, supplier_id)
);

create table supplier_price_history (
    id bigserial primary key,
    organisation_id bigint not null,
    product_id bigint not null references products(id) on delete cascade,
    supplier_id bigint not null references suppliers(id) on delete cascade,
    unit_cost numeric(12,2) not null,
    source varchar(40) not null,
    observed_at timestamp not null
);

create index idx_supplier_price_history_product on supplier_price_history(organisation_id, product_id, observed_at desc);
