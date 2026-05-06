create table if not exists users (
    id bigint primary key,
    nickname varchar(100),
    avatar_url varchar(500),
    status varchar(30) not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp
);

create table if not exists organizations (
    id bigint primary key,
    name varchar(200) not null,
    provider varchar(50) not null,
    org_type varchar(50) not null,
    tenant_key varchar(200) not null,
    status varchar(30) not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp,
    constraint uk_organizations_provider_tenant unique (provider, tenant_key)
);

create table if not exists user_identities (
    id bigint primary key,
    user_id bigint not null,
    org_id bigint null,
    provider varchar(50) not null,
    provider_id varchar(200) not null,
    tenant_key varchar(200) not null,
    extra_info text,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp,
    constraint fk_user_identities_user foreign key (user_id) references users(id),
    constraint fk_user_identities_org foreign key (org_id) references organizations(id),
    constraint uk_user_identities_provider_tenant_provider_id unique (provider, tenant_key, provider_id)
);

create table if not exists user_orgs (
    id bigint primary key,
    user_id bigint not null,
    org_id bigint not null,
    role varchar(50) not null,
    status varchar(30) not null,
    joined_at datetime not null default current_timestamp,
    left_at datetime,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp,
    constraint fk_user_orgs_user foreign key (user_id) references users(id),
    constraint fk_user_orgs_org foreign key (org_id) references organizations(id),
    constraint uk_user_orgs_user_org unique (user_id, org_id)
);

create table if not exists local_accounts (
    user_id bigint primary key,
    username varchar(100) not null unique,
    password_hash varchar(500) not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp,
    constraint fk_local_accounts_user foreign key (user_id) references users(id)
);

create table if not exists web_auth_sessions (
    token varchar(100) primary key,
    user_id bigint not null,
    expires_at datetime not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp,
    constraint fk_web_auth_sessions_user foreign key (user_id) references users(id)
);

create table if not exists subjects (
    id bigint primary key,
    org_id bigint not null,
    type varchar(30) not null,
    provider varchar(50) not null,
    ref_id varchar(200) not null,
    name varchar(200),
    status varchar(30) not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp,
    constraint fk_subjects_org foreign key (org_id) references organizations(id),
    constraint uk_subjects_org_id unique (org_id, id),
    constraint uk_subjects_org_type_provider_ref unique (org_id, type, provider, ref_id)
);

create table if not exists group_members (
    id bigint primary key,
    org_id bigint not null,
    group_subject_id bigint not null,
    user_id bigint not null,
    role varchar(50) not null,
    status varchar(30) not null,
    joined_at datetime not null default current_timestamp,
    left_at datetime,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp,
    constraint fk_group_members_org foreign key (org_id) references organizations(id),
    constraint fk_group_members_org_subject foreign key (org_id, group_subject_id) references subjects(org_id, id),
    constraint fk_group_members_user foreign key (user_id) references users(id),
    constraint uk_group_members_group_user unique (group_subject_id, user_id)
);

create table if not exists conversations (
    id bigint primary key,
    org_id bigint not null,
    subject_id bigint not null,
    title varchar(300),
    source varchar(50) not null,
    status varchar(30) not null,
    created_by_user_id bigint,
    last_message_at datetime not null default current_timestamp,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp,
    constraint fk_conversations_org foreign key (org_id) references organizations(id),
    constraint fk_conversations_subject foreign key (org_id, subject_id) references subjects(org_id, id),
    constraint uk_conversations_org_id unique (org_id, id)
);

create table if not exists messages (
    id bigint primary key,
    org_id bigint not null,
    conversation_id bigint not null,
    sender_user_id bigint,
    role varchar(30) not null,
    content text not null,
    message_type varchar(50) not null,
    provider_message_id varchar(200),
    created_at datetime not null default current_timestamp,
    constraint fk_messages_org foreign key (org_id) references organizations(id),
    constraint fk_messages_conversation foreign key (org_id, conversation_id) references conversations(org_id, id),
    constraint fk_messages_sender foreign key (sender_user_id) references users(id),
    constraint uk_messages_provider_message_org unique (provider_message_id, org_id)
);

create table if not exists memory (
    id bigint primary key,
    org_id bigint not null,
    subject_id bigint not null,
    memory_type varchar(50) not null,
    scope varchar(50) not null,
    content text not null,
    embedding_id varchar(200),
    source_conversation_id bigint,
    created_by_user_id bigint,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp,
    constraint fk_memory_org foreign key (org_id) references organizations(id),
    constraint fk_memory_subject foreign key (org_id, subject_id) references subjects(org_id, id),
    constraint fk_memory_source_conversation foreign key (org_id, source_conversation_id) references conversations(org_id, id),
    constraint fk_memory_created_by_user foreign key (created_by_user_id) references users(id)
);

create index idx_conversations_org_subject_last
    on conversations (org_id, subject_id, last_message_at);

create index idx_conversations_org_last
    on conversations (org_id, last_message_at);

create index idx_messages_org_conversation_created
    on messages (org_id, conversation_id, created_at);

create index idx_memory_org_subject
    on memory (org_id, subject_id);

create index idx_memory_org_type_scope
    on memory (org_id, memory_type, scope);

create index idx_local_accounts_username
    on local_accounts (username);

create index idx_web_auth_sessions_user
    on web_auth_sessions (user_id);
