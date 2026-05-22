alter table billing_subscriptions
    drop constraint fk_billing_subscriptions_organisation;

alter table billing_subscriptions
    add constraint fk_billing_subscriptions_organisation
        foreign key (organisation_id) references organisations(id) on delete cascade;
