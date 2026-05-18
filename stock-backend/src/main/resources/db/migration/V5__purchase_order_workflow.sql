alter table purchase_order_lines
    add column received_quantity integer not null default 0;

alter table purchase_order_lines
    add constraint ck_purchase_order_lines_received_quantity
        check (received_quantity >= 0 and received_quantity <= quantity);

alter table ai_reorder_recommendations
    add column purchase_order_id bigint;

alter table ai_reorder_recommendations
    add constraint fk_ai_reorder_recommendations_purchase_order
        foreign key (purchase_order_id) references purchase_orders (id);

create index idx_ai_reorder_recommendations_purchase_order
    on ai_reorder_recommendations (purchase_order_id);
