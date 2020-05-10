
--
-- Name: auth; Type: SCHEMA; Schema: -; Owner: aaa
--

CREATE SCHEMA IF NOT EXISTS auth;


--
-- Name: images; Type: SCHEMA; Schema: -; Owner: aaa
--

CREATE SCHEMA IF NOT EXISTS images;


--
-- Name: user_creation_type; Type: TYPE; Schema: auth; Owner: aaa
--

CREATE TYPE auth.user_creation_type AS ENUM (
    'REGISTRATION',
    'FACEBOOK',
    'VKONTAKTE'
);


--
-- Name: user_role; Type: TYPE; Schema: auth; Owner: aaa
--

CREATE TYPE auth.user_role AS ENUM (
    'ROLE_ADMIN',
    'ROLE_USER'
);


--
-- Name: CAST (character varying AS auth.user_creation_type); Type: CAST; Schema: -; Owner: -
--

CREATE CAST (character varying AS auth.user_creation_type) WITH INOUT AS ASSIGNMENT;


--
-- Name: CAST (character varying AS auth.user_role); Type: CAST; Schema: -; Owner: -
--

CREATE CAST (character varying AS auth.user_role) WITH INOUT AS ASSIGNMENT;


--
-- Name: users; Type: TABLE; Schema: auth; Owner: aaa
--

CREATE TABLE auth.users (
    id bigserial PRIMARY KEY,
    username character varying(50) UNIQUE NOT NULL,
    password character varying(100),
    avatar character varying(256),
    enabled boolean DEFAULT true NOT NULL,
    expired boolean DEFAULT false NOT NULL,
    locked boolean DEFAULT false NOT NULL,
    email character varying(100) UNIQUE,
    role auth.user_role DEFAULT 'ROLE_USER'::auth.user_role NOT NULL,
    creation_type auth.user_creation_type DEFAULT 'REGISTRATION'::auth.user_creation_type NOT NULL,
    facebook_id character varying(64) UNIQUE,
    vkontakte_id character varying(64) UNIQUE,
    last_login_date_time timestamp without time zone
);



--
-- Name: user_avatar_image; Type: TABLE; Schema: images; Owner: aaa
--

CREATE TABLE images.user_avatar_image (
	id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    img bytea,
    content_type character varying(64),
    create_date_time timestamp without time zone DEFAULT timezone('utc'::text, now()) NOT NULL
);



--
-- Data for Name: users; Type: TABLE DATA; Schema: auth; Owner: aaa
--

INSERT INTO auth.users (id, username, password, avatar, enabled, expired, locked, email, role, creation_type, facebook_id, vkontakte_id, last_login_date_time) VALUES
 (-1, 'deleted', NULL, NULL, false, true, true, NULL, 'ROLE_USER', 'REGISTRATION', NULL, NULL, NULL),
 (1, 'admin', '$2a$10$HsyFGy9IO//nJZxYc2xjDeV/kF7koiPrgIDzPOfgmngKVe9cOyOS2', 'https://cdn3.iconfinder.com/data/icons/rcons-user-action/32/boy-512.png', true, false, false, 'admin@example.com', 'ROLE_ADMIN', 'REGISTRATION', NULL, NULL, NULL);


SELECT setval('auth.users_id_seq', (SELECT MAX(id) FROM auth.users));