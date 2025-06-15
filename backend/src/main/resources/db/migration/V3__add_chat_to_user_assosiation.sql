CREATE TABLE chat_to_user_assosiation
(
    chat_uuid TEXT   NOT NULL,
    order_id  BIGINT NOT NULL
);

ALTER TABLE chat_to_user_assosiation
    ADD CONSTRAINT fk_chatouseass_on_telegram_chat FOREIGN KEY (chat_uuid) REFERENCES telegram_chats (chat_uuid);

ALTER TABLE chat_to_user_assosiation
    ADD CONSTRAINT fk_chatouseass_on_user FOREIGN KEY (order_id) REFERENCES users (id);