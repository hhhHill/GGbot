create table if not exists users (
    id bigint primary key,
    display_name varchar(255) not null,
    status varchar(32) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table if not exists organizations (
    id bigint primary key,
    provider varchar(32) not null,
    tenant_key varchar(255) not null,
    name varchar(255) not null,
    org_type varchar(32) not null,
    status varchar(32) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_organizations_provider_tenant unique (provider, tenant_key)
);

create table if not exists user_identities (
    id bigint primary key,
    user_id bigint not null,
    provider varchar(32) not null,
    tenant_key varchar(255) not null,
    provider_id varchar(255) not null,
    provider_union_id varchar(255),
    provider_name varchar(255),
    status varchar(32) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_user_identities_user foreign key (user_id) references users(id),
    constraint uk_user_identities_provider_tenant_id unique (provider, tenant_key, provider_id)
);

create table if not exists user_orgs (
    id bigint primary key,
    user_id bigint not null,
    org_id bigint not null,
    role varchar(32) not null,
    status varchar(32) not null,
    joined_at timestamp not null default current_timestamp,
    left_at timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_user_orgs_user foreign key (user_id) references users(id),
    constraint fk_user_orgs_org foreign key (org_id) references organizations(id),
    constraint uk_user_orgs_user_org unique (user_id, org_id)
);

create table if not exists subjects (
    id bigint primary key,
    org_id bigint not null,
    type varchar(32) not null,
    provider varchar(32) not null,
    ref_id varchar(255) not null,
    name varchar(255) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_subjects_org foreign key (org_id) references organizations(id),
    constraint uk_subjects_org_type_provider_ref unique (org_id, type, provider, ref_id)
);

create table if not exists group_members (
    id bigint primary key,
    org_id bigint not null,
    group_subject_id bigint not null,
    user_id bigint not null,
    status varchar(32) not null,
    joined_at timestamp not null default current_timestamp,
    left_at timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_group_members_org foreign key (org_id) references organizations(id),
    constraint fk_group_members_subject foreign key (group_subject_id) references subjects(id),
    constraint fk_group_members_user foreign key (user_id) references users(id),
    constraint uk_group_members_subject_user unique (group_subject_id, user_id)
);

create table if not exists conversations (
    id bigint primary key,
    org_id bigint not null,
    subject_id bigint not null,
    title varchar(255),
    status varchar(32) not null,
    last_message_at timestamp not null default current_timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_conversations_org foreign key (org_id) references organizations(id),
    constraint fk_conversations_subject foreign key (subject_id) references subjects(id)
);

create table if not exists messages (
    id bigint primary key,
    org_id bigint not null,
    conversation_id bigint not null,
    subject_id bigint not null,
    role varchar(32) not null,
    provider_message_id varchar(255),
    content text not null,
    created_at timestamp not null default current_timestamp,
    constraint fk_messages_org foreign key (org_id) references organizations(id),
    constraint fk_messages_conversation foreign key (conversation_id) references conversations(id),
    constraint fk_messages_subject foreign key (subject_id) references subjects(id),
    constraint uk_messages_org_provider_message unique (org_id, provider_message_id)
);

create table if not exists memory (
    id bigint primary key,
    org_id bigint not null,
    subject_id bigint not null,
    memory_type varchar(32) not null,
    scope varchar(32) not null,
    content text not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_memory_org foreign key (org_id) references organizations(id),
    constraint fk_memory_subject foreign key (subject_id) references subjects(id)
);

create index if not exists idx_conversations_org_subject_last_message_at
    on conversations (org_id, subject_id, last_message_at);

create index if not exists idx_conversations_org_last_message_at
    on conversations (org_id, last_message_at);

create index if not exists idx_messages_org_conversation_created_at
    on messages (org_id, conversation_id, created_at);

create index if not exists idx_memory_org_subject
    on memory (org_id, subject_id);

create index if not exists idx_memory_org_type_scope
    on memory (org_id, memory_type, scope);
