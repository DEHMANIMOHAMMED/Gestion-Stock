alter table purchase_orders
    drop constraint ck_purchase_orders_status;

alter table purchase_orders
    add constraint ck_purchase_orders_status
        check (status in ('DRAFT', 'APPROVED', 'ORDERED', 'RECEIVED', 'CANCELLED'));
