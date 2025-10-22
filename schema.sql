--
-- PostgreSQL database dump
--

-- Dumped from database version 16.9 (Debian 16.9-1.pgdg120+1)
-- Dumped by pg_dump version 16.9 (Debian 16.9-1.pgdg120+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: agreement; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.agreement (
    created_at date NOT NULL,
    marketing boolean NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.agreement OWNER TO melog_user;

--
-- Name: block; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.block (
    blocked_at timestamp(6) without time zone NOT NULL,
    blocked_id uuid NOT NULL,
    blocker_id uuid NOT NULL,
    id uuid NOT NULL
);


ALTER TABLE public.block OWNER TO melog_user;

--
-- Name: calendars; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.calendars (
    created_at date NOT NULL,
    end_date date,
    start_date date NOT NULL,
    id uuid NOT NULL,
    source character varying(40) NOT NULL,
    classification character varying(100),
    external_id character varying(100) NOT NULL,
    region character varying(100),
    title character varying(500) NOT NULL,
    detail_url character varying(1000),
    image_url character varying(1000),
    description text
);


ALTER TABLE public.calendars OWNER TO melog_user;

--
-- Name: comment_likes; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.comment_likes (
    comment_id uuid NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.comment_likes OWNER TO melog_user;

--
-- Name: composers; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.composers (
    onboarding_id uuid NOT NULL,
    composers character varying(255)
);


ALTER TABLE public.composers OWNER TO melog_user;

--
-- Name: event_alarm; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.event_alarm (
    alarm_time time(6) without time zone NOT NULL,
    enabled boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    event_schedule_id uuid NOT NULL,
    id uuid NOT NULL,
    status character varying(20) NOT NULL,
    CONSTRAINT event_alarm_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SENT'::character varying, 'CANCELLED'::character varying])::text[])))
);


ALTER TABLE public.event_alarm OWNER TO melog_user;

--
-- Name: event_schedule; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.event_schedule (
    event_date date NOT NULL,
    calendar_id uuid NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.event_schedule OWNER TO melog_user;

--
-- Name: follow; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.follow (
    followed_at timestamp(6) without time zone NOT NULL,
    follower uuid,
    following uuid,
    id uuid NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT follow_status_check CHECK (((status)::text = ANY ((ARRAY['REQUESTED'::character varying, 'ACCEPTED'::character varying, 'BLOCKED'::character varying, 'UNFOLLOW'::character varying])::text[])))
);


ALTER TABLE public.follow OWNER TO melog_user;

--
-- Name: harmony_bookmarks; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.harmony_bookmarks (
    bookmarked_at timestamp(6) without time zone NOT NULL,
    harmony_room_id uuid NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.harmony_bookmarks OWNER TO melog_user;

--
-- Name: harmony_reports; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.harmony_reports (
    reported_at timestamp(6) without time zone NOT NULL,
    harmony_room_id uuid NOT NULL,
    id uuid NOT NULL,
    reporter_id uuid NOT NULL,
    category character varying(50) NOT NULL,
    reason character varying(50) NOT NULL,
    details text
);


ALTER TABLE public.harmony_reports OWNER TO melog_user;

--
-- Name: harmony_room_assign_wait; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.harmony_room_assign_wait (
    created_at timestamp(6) without time zone NOT NULL,
    harmony_room_id uuid NOT NULL,
    id uuid NOT NULL
);


ALTER TABLE public.harmony_room_assign_wait OWNER TO melog_user;

--
-- Name: harmony_room_members; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.harmony_room_members (
    joined_at timestamp(6) without time zone NOT NULL,
    harmony_room_id uuid NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    role character varying(20) NOT NULL
);


ALTER TABLE public.harmony_room_members OWNER TO melog_user;

--
-- Name: harmony_room_posts; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.harmony_room_posts (
    created_at timestamp(6) without time zone NOT NULL,
    harmony_room_id uuid NOT NULL,
    id uuid NOT NULL,
    post_ids json
);


ALTER TABLE public.harmony_room_posts OWNER TO melog_user;

--
-- Name: harmony_room_waiting_users; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.harmony_room_waiting_users (
    assign_wait_id uuid NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.harmony_room_waiting_users OWNER TO melog_user;

--
-- Name: harmony_rooms; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.harmony_rooms (
    book_mark_num integer NOT NULL,
    is_direct_assign boolean NOT NULL,
    is_private boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    owner_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    intro text,
    profile_image_url character varying(255),
    category json
);


ALTER TABLE public.harmony_rooms OWNER TO melog_user;

--
-- Name: inquiries; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.inquiries (
    created_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    child_type character varying(32) NOT NULL,
    parent_type character varying(32) NOT NULL,
    title character varying(150) NOT NULL,
    content oid NOT NULL,
    CONSTRAINT inquiries_child_type_check CHECK (((child_type)::text = ANY ((ARRAY['USAGE'::character varying, 'BUG'::character varying, 'ACCOUNT_PRIVACY'::character varying, 'POLICY'::character varying, 'PARTNERSHIP'::character varying, 'MARKETING'::character varying, 'CONTENT'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT inquiries_parent_type_check CHECK (((parent_type)::text = ANY ((ARRAY['ACCOUNT'::character varying, 'SUGGESTION'::character varying, 'OTHER'::character varying])::text[])))
);


ALTER TABLE public.inquiries OWNER TO melog_user;

--
-- Name: instruments; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.instruments (
    onboarding_id uuid NOT NULL,
    instruments character varying(255)
);


ALTER TABLE public.instruments OWNER TO melog_user;

--
-- Name: notices; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.notices (
    is_important boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    category character varying(50),
    title character varying(200) NOT NULL,
    image_url character varying(500),
    content text NOT NULL
);


ALTER TABLE public.notices OWNER TO melog_user;

--
-- Name: onboarding; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.onboarding (
    onboarding_id uuid NOT NULL,
    user_id uuid
);


ALTER TABLE public.onboarding OWNER TO melog_user;

--
-- Name: periods; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.periods (
    onboarding_id uuid NOT NULL,
    periods character varying(255)
);


ALTER TABLE public.periods OWNER TO melog_user;

--
-- Name: post_bookmarks; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.post_bookmarks (
    created_at date NOT NULL,
    post_id uuid NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.post_bookmarks OWNER TO melog_user;

--
-- Name: post_comments; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.post_comments (
    created_at date NOT NULL,
    id uuid NOT NULL,
    parent_comment_id uuid,
    post_id uuid NOT NULL,
    user_id uuid NOT NULL,
    content text NOT NULL
);


ALTER TABLE public.post_comments OWNER TO melog_user;

--
-- Name: post_hidden_users; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.post_hidden_users (
    post_id uuid NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.post_hidden_users OWNER TO melog_user;

--
-- Name: post_likes; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.post_likes (
    post_id uuid NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.post_likes OWNER TO melog_user;

--
-- Name: posts; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.posts (
    created_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    content text,
    media_type character varying(255),
    media_url character varying(255),
    title character varying(255) NOT NULL,
    tags json
);


ALTER TABLE public.posts OWNER TO melog_user;

--
-- Name: reports; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.reports (
    created_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    reporter_id uuid NOT NULL,
    target_id uuid NOT NULL,
    target_type character varying(20) NOT NULL,
    category character varying(50) NOT NULL,
    reason character varying(100) NOT NULL,
    details text
);


ALTER TABLE public.reports OWNER TO melog_user;

--
-- Name: user_profile_music; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.user_profile_music (
    is_active boolean NOT NULL,
    selected_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    youtube_video_id character varying(32),
    thumbnail_url character varying(255),
    youtube_url character varying(255),
    title character varying(255),
    description character varying(255)
);


ALTER TABLE public.user_profile_music OWNER TO melog_user;

--
-- Name: users; Type: TABLE; Schema: public; Owner: melog_user
--

CREATE TABLE public.users (
    active boolean,
    id uuid NOT NULL,
    email character varying(255) NOT NULL,
    intro character varying(255),
    nickname character varying(255),
    oidc character varying(255) NOT NULL,
    platform character varying(255) NOT NULL,
    profile_image_url character varying(255),
    CONSTRAINT users_platform_check CHECK (((platform)::text = ANY ((ARRAY['KAKAO'::character varying, 'GOOGLE'::character varying, 'NAVER'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO melog_user;

--
-- Name: agreement agreement_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.agreement
    ADD CONSTRAINT agreement_pkey PRIMARY KEY (user_id);


--
-- Name: block block_blocker_id_blocked_id_key; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.block
    ADD CONSTRAINT block_blocker_id_blocked_id_key UNIQUE (blocker_id, blocked_id);


--
-- Name: block block_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.block
    ADD CONSTRAINT block_pkey PRIMARY KEY (id);


--
-- Name: calendars calendars_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.calendars
    ADD CONSTRAINT calendars_pkey PRIMARY KEY (id);


--
-- Name: event_alarm event_alarm_event_schedule_id_key; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.event_alarm
    ADD CONSTRAINT event_alarm_event_schedule_id_key UNIQUE (event_schedule_id);


--
-- Name: event_alarm event_alarm_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.event_alarm
    ADD CONSTRAINT event_alarm_pkey PRIMARY KEY (id);


--
-- Name: event_schedule event_schedule_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.event_schedule
    ADD CONSTRAINT event_schedule_pkey PRIMARY KEY (id);


--
-- Name: follow follow_follower_following_key; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.follow
    ADD CONSTRAINT follow_follower_following_key UNIQUE (follower, following);


--
-- Name: follow follow_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.follow
    ADD CONSTRAINT follow_pkey PRIMARY KEY (id);


--
-- Name: harmony_bookmarks harmony_bookmarks_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_bookmarks
    ADD CONSTRAINT harmony_bookmarks_pkey PRIMARY KEY (id);


--
-- Name: harmony_bookmarks harmony_bookmarks_user_id_harmony_room_id_key; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_bookmarks
    ADD CONSTRAINT harmony_bookmarks_user_id_harmony_room_id_key UNIQUE (user_id, harmony_room_id);


--
-- Name: harmony_reports harmony_reports_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_reports
    ADD CONSTRAINT harmony_reports_pkey PRIMARY KEY (id);


--
-- Name: harmony_room_assign_wait harmony_room_assign_wait_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_room_assign_wait
    ADD CONSTRAINT harmony_room_assign_wait_pkey PRIMARY KEY (id);


--
-- Name: harmony_room_members harmony_room_members_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_room_members
    ADD CONSTRAINT harmony_room_members_pkey PRIMARY KEY (id);


--
-- Name: harmony_room_posts harmony_room_posts_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_room_posts
    ADD CONSTRAINT harmony_room_posts_pkey PRIMARY KEY (id);


--
-- Name: harmony_rooms harmony_rooms_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_rooms
    ADD CONSTRAINT harmony_rooms_pkey PRIMARY KEY (id);


--
-- Name: inquiries inquiries_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.inquiries
    ADD CONSTRAINT inquiries_pkey PRIMARY KEY (id);


--
-- Name: notices notices_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.notices
    ADD CONSTRAINT notices_pkey PRIMARY KEY (id);


--
-- Name: onboarding onboarding_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.onboarding
    ADD CONSTRAINT onboarding_pkey PRIMARY KEY (onboarding_id);


--
-- Name: onboarding onboarding_user_id_key; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.onboarding
    ADD CONSTRAINT onboarding_user_id_key UNIQUE (user_id);


--
-- Name: post_bookmarks post_bookmarks_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.post_bookmarks
    ADD CONSTRAINT post_bookmarks_pkey PRIMARY KEY (post_id, user_id);


--
-- Name: post_comments post_comments_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.post_comments
    ADD CONSTRAINT post_comments_pkey PRIMARY KEY (id);


--
-- Name: posts posts_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT posts_pkey PRIMARY KEY (id);


--
-- Name: reports reports_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_pkey PRIMARY KEY (id);


--
-- Name: block ukci08931wigbpc2octj27oldvu; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.block
    ADD CONSTRAINT ukci08931wigbpc2octj27oldvu UNIQUE (blocker_id, blocked_id);


--
-- Name: harmony_bookmarks ukhdaowes2jc4jn43t6rxx9e3ab; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_bookmarks
    ADD CONSTRAINT ukhdaowes2jc4jn43t6rxx9e3ab UNIQUE (user_id, harmony_room_id);


--
-- Name: follow ukjum3i378oc83arnjbw7bm0c0p; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.follow
    ADD CONSTRAINT ukjum3i378oc83arnjbw7bm0c0p UNIQUE (follower, following);


--
-- Name: user_profile_music user_profile_music_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.user_profile_music
    ADD CONSTRAINT user_profile_music_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: event_alarm ux_alarm_schedule; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.event_alarm
    ADD CONSTRAINT ux_alarm_schedule UNIQUE (event_schedule_id);


--
-- Name: calendars ux_calendar_source_external; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.calendars
    ADD CONSTRAINT ux_calendar_source_external UNIQUE (source, external_id);


--
-- Name: event_schedule ux_event_schedule_user_calendar_date; Type: CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.event_schedule
    ADD CONSTRAINT ux_event_schedule_user_calendar_date UNIQUE (user_id, calendar_id, event_date);


--
-- Name: idx_inquiry_parent_child; Type: INDEX; Schema: public; Owner: melog_user
--

CREATE INDEX idx_inquiry_parent_child ON public.inquiries USING btree (parent_type, child_type);


--
-- Name: idx_inquiry_user_created_at; Type: INDEX; Schema: public; Owner: melog_user
--

CREATE INDEX idx_inquiry_user_created_at ON public.inquiries USING btree (user_id, created_at);


--
-- Name: idx_upm_user; Type: INDEX; Schema: public; Owner: melog_user
--

CREATE INDEX idx_upm_user ON public.user_profile_music USING btree (user_id);


--
-- Name: idx_upm_user_active; Type: INDEX; Schema: public; Owner: melog_user
--

CREATE INDEX idx_upm_user_active ON public.user_profile_music USING btree (user_id, is_active);


--
-- Name: harmony_bookmarks fk170qicdgcgqggqie9hhsik8u4; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_bookmarks
    ADD CONSTRAINT fk170qicdgcgqggqie9hhsik8u4 FOREIGN KEY (harmony_room_id) REFERENCES public.harmony_rooms(id);


--
-- Name: periods fk1fxkwkrxhay23vc17j9ubtr40; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.periods
    ADD CONSTRAINT fk1fxkwkrxhay23vc17j9ubtr40 FOREIGN KEY (onboarding_id) REFERENCES public.onboarding(onboarding_id);


--
-- Name: harmony_rooms fk1jaqtsewgdodrflsv38ctg2ky; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_rooms
    ADD CONSTRAINT fk1jaqtsewgdodrflsv38ctg2ky FOREIGN KEY (owner_id) REFERENCES public.users(id);


--
-- Name: harmony_bookmarks fk1nebgm4fdtmfc9g4jm75b2hvp; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_bookmarks
    ADD CONSTRAINT fk1nebgm4fdtmfc9g4jm75b2hvp FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: post_comments fk21q7y8a124im4g0l4aaxn4ol1; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.post_comments
    ADD CONSTRAINT fk21q7y8a124im4g0l4aaxn4ol1 FOREIGN KEY (parent_comment_id) REFERENCES public.post_comments(id);


--
-- Name: event_schedule fk2t43e39by1scgc9p99erttw1d; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.event_schedule
    ADD CONSTRAINT fk2t43e39by1scgc9p99erttw1d FOREIGN KEY (calendar_id) REFERENCES public.calendars(id);


--
-- Name: block fk4txnfd3qo7l9ohqe04rl1dqxu; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.block
    ADD CONSTRAINT fk4txnfd3qo7l9ohqe04rl1dqxu FOREIGN KEY (blocker_id) REFERENCES public.users(id);


--
-- Name: harmony_room_posts fk5fqpp4qqfhnfn0rwqwbmqs2i1; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_room_posts
    ADD CONSTRAINT fk5fqpp4qqfhnfn0rwqwbmqs2i1 FOREIGN KEY (harmony_room_id) REFERENCES public.harmony_rooms(id);


--
-- Name: posts fk5lidm6cqbc7u4xhqpxm898qme; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT fk5lidm6cqbc7u4xhqpxm898qme FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: comment_likes fk6h3lbneryl5pyb9ykaju7werx; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.comment_likes
    ADD CONSTRAINT fk6h3lbneryl5pyb9ykaju7werx FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: comment_likes fk6q90s6gsqjadphkngyf3y0fop; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.comment_likes
    ADD CONSTRAINT fk6q90s6gsqjadphkngyf3y0fop FOREIGN KEY (comment_id) REFERENCES public.post_comments(id);


--
-- Name: post_bookmarks fk9b5c09u5arho7ei76d78bn7ww; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.post_bookmarks
    ADD CONSTRAINT fk9b5c09u5arho7ei76d78bn7ww FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: post_likes fka5wxsgl4doibhbed9gm7ikie2; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.post_likes
    ADD CONSTRAINT fka5wxsgl4doibhbed9gm7ikie2 FOREIGN KEY (post_id) REFERENCES public.posts(id);


--
-- Name: post_comments fkaawaqxjs3br8dw5v90w7uu514; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.post_comments
    ADD CONSTRAINT fkaawaqxjs3br8dw5v90w7uu514 FOREIGN KEY (post_id) REFERENCES public.posts(id);


--
-- Name: follow fkao9nuo6wos23rxdee80jvh1vw; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.follow
    ADD CONSTRAINT fkao9nuo6wos23rxdee80jvh1vw FOREIGN KEY (following) REFERENCES public.users(id);


--
-- Name: harmony_reports fkawnp335aawhlifancinmtx3e4; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_reports
    ADD CONSTRAINT fkawnp335aawhlifancinmtx3e4 FOREIGN KEY (reporter_id) REFERENCES public.users(id);


--
-- Name: user_profile_music fkb7lvrvac8wmcysyp80jo6gp4f; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.user_profile_music
    ADD CONSTRAINT fkb7lvrvac8wmcysyp80jo6gp4f FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: post_bookmarks fkclpw1l6wrci96rfj0dtt3bfah; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.post_bookmarks
    ADD CONSTRAINT fkclpw1l6wrci96rfj0dtt3bfah FOREIGN KEY (post_id) REFERENCES public.posts(id);


--
-- Name: harmony_room_members fkcv72vwbxpit2rmgsh1u1or2g2; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_room_members
    ADD CONSTRAINT fkcv72vwbxpit2rmgsh1u1or2g2 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: reports fkd3qiw2om5d2oh5xb7fbdcq225; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT fkd3qiw2om5d2oh5xb7fbdcq225 FOREIGN KEY (reporter_id) REFERENCES public.users(id);


--
-- Name: harmony_room_members fkede12alecwgpk5pebkp1umru4; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_room_members
    ADD CONSTRAINT fkede12alecwgpk5pebkp1umru4 FOREIGN KEY (harmony_room_id) REFERENCES public.harmony_rooms(id);


--
-- Name: block fkev0ppco3q86uc17mujrv3yg0; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.block
    ADD CONSTRAINT fkev0ppco3q86uc17mujrv3yg0 FOREIGN KEY (blocked_id) REFERENCES public.users(id);


--
-- Name: inquiries fkfks94q8sobcuibrudbr3im380; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.inquiries
    ADD CONSTRAINT fkfks94q8sobcuibrudbr3im380 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: instruments fkfq3mmvtos55g0mw7650w0ed6u; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.instruments
    ADD CONSTRAINT fkfq3mmvtos55g0mw7650w0ed6u FOREIGN KEY (onboarding_id) REFERENCES public.onboarding(onboarding_id);


--
-- Name: composers fkgm2byv7wtgshir8kt209dj4fj; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.composers
    ADD CONSTRAINT fkgm2byv7wtgshir8kt209dj4fj FOREIGN KEY (onboarding_id) REFERENCES public.onboarding(onboarding_id);


--
-- Name: event_schedule fkhnwls26ogdjgs9tu64ufkj6j4; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.event_schedule
    ADD CONSTRAINT fkhnwls26ogdjgs9tu64ufkj6j4 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: post_hidden_users fkiaeljla2rql4d9w3fjy5aip36; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.post_hidden_users
    ADD CONSTRAINT fkiaeljla2rql4d9w3fjy5aip36 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: onboarding fkinpyt4ngxlboj8ev3pe2e1cff; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.onboarding
    ADD CONSTRAINT fkinpyt4ngxlboj8ev3pe2e1cff FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: harmony_room_waiting_users fkiqdmrexcu6yx2w5mktcqlnff8; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_room_waiting_users
    ADD CONSTRAINT fkiqdmrexcu6yx2w5mktcqlnff8 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: event_alarm fkj9vttw0oorqc95fkbunr14aba; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.event_alarm
    ADD CONSTRAINT fkj9vttw0oorqc95fkbunr14aba FOREIGN KEY (event_schedule_id) REFERENCES public.event_schedule(id);


--
-- Name: post_likes fkkgau5n0nlewg6o9lr4yibqgxj; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.post_likes
    ADD CONSTRAINT fkkgau5n0nlewg6o9lr4yibqgxj FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: harmony_room_waiting_users fkqksu3dmrj03qdmhre7pmlopa8; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_room_waiting_users
    ADD CONSTRAINT fkqksu3dmrj03qdmhre7pmlopa8 FOREIGN KEY (assign_wait_id) REFERENCES public.harmony_room_assign_wait(id);


--
-- Name: follow fkroel3ecs669qrrhrtfi3ta0yr; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.follow
    ADD CONSTRAINT fkroel3ecs669qrrhrtfi3ta0yr FOREIGN KEY (follower) REFERENCES public.users(id);


--
-- Name: harmony_reports fkrrv5edfkqjqch3eo8o809n8dd; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_reports
    ADD CONSTRAINT fkrrv5edfkqjqch3eo8o809n8dd FOREIGN KEY (harmony_room_id) REFERENCES public.harmony_rooms(id);


--
-- Name: post_hidden_users fkrugkegs47o5ewtqa5c3vojlbv; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.post_hidden_users
    ADD CONSTRAINT fkrugkegs47o5ewtqa5c3vojlbv FOREIGN KEY (post_id) REFERENCES public.posts(id);


--
-- Name: agreement fksihbk94e9up66g55uc4gw4ip1; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.agreement
    ADD CONSTRAINT fksihbk94e9up66g55uc4gw4ip1 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: post_comments fksnxoecngu89u3fh4wdrgf0f2g; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.post_comments
    ADD CONSTRAINT fksnxoecngu89u3fh4wdrgf0f2g FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: harmony_room_assign_wait fktht6rl2kxj0eokco59f0neu12; Type: FK CONSTRAINT; Schema: public; Owner: melog_user
--

ALTER TABLE ONLY public.harmony_room_assign_wait
    ADD CONSTRAINT fktht6rl2kxj0eokco59f0neu12 FOREIGN KEY (harmony_room_id) REFERENCES public.harmony_rooms(id);


--
-- PostgreSQL database dump complete
--

