alter table organisations add column industry varchar(80);
alter table organisations add column size_range varchar(40);
alter table organisations add column phone varchar(60);
alter table organisations add column address varchar(220);
alter table organisations add column city varchar(120);
alter table organisations add column country varchar(120);
alter table organisations add column currency varchar(10);
alter table organisations add column onboarding_completed boolean not null default false;

alter table admin_notifications add column status varchar(20) not null default 'OPEN';
alter table admin_notifications add column action_taken varchar(60);
alter table admin_notifications add column actioned_at timestamp;
alter table admin_notifications add column actioned_by_user_id bigint;
alter table admin_notifications add column dismissal_reason varchar(600);

alter table admin_notifications
    add constraint ck_admin_notifications_status
        check (status in ('OPEN', 'ACTIONED', 'DISMISSED'));

create index idx_admin_notifications_org_status_created
    on admin_notifications (organisation_id, status, created_at);
