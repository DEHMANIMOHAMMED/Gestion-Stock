create table ai_copilot_conversations (
    id bigserial primary key,
    organisation_id bigint not null,
    user_id bigint not null,
    title varchar(180) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_ai_copilot_conversations_org on ai_copilot_conversations(organisation_id, updated_at desc);

create table ai_copilot_messages (
    id bigserial primary key,
    organisation_id bigint not null,
    conversation_id bigint not null references ai_copilot_conversations(id) on delete cascade,
    user_id bigint not null,
    question varchar(1000) not null,
    answer varchar(4000) not null,
    source varchar(40) not null,
    citations varchar(3000),
    created_at timestamp not null
);

create index idx_ai_copilot_messages_conversation on ai_copilot_messages(organisation_id, conversation_id, created_at desc);

alter table ai_audit_logs add column if not exists actor_email varchar(180);
alter table ai_audit_logs add column if not exists target_type varchar(80);
alter table ai_audit_logs add column if not exists target_id bigint;
alter table ai_audit_logs add column if not exists source varchar(40);
alter table ai_audit_logs add column if not exists summary varchar(1200);

create index if not exists idx_ai_audit_logs_org on ai_audit_logs(organisation_id, created_at desc);
