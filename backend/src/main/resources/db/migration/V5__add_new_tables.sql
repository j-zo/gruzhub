CREATE TABLE transport_column
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    column_number       TEXT
);

CREATE TABLE mechanic
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transport_column_id UUID,
    name TEXT,
    phone TEXT,
    email TEXT
);
ALTER TABLE mechanic
    ADD CONSTRAINT FK_MECHANIC_ON_TRANSPORT_COLUMN FOREIGN KEY (transport_column_id) REFERENCES transport_column (id);

-- Rename auto to transport
ALTER TABLE auto RENAME TO transport;
ALTER TABLE transport RENAME CONSTRAINT FK_AUTO_ON_CUSTOMER TO FK_TRANSPORT_ON_CUSTOMER;

--- DRIVERS ------
CREATE TABLE driver
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    old_id BIGINT,
    transport_id BIGINT,
    name TEXT,
    phone TEXT,
    email TEXT
);

--- Add new id column for a driver
ALTER TABLE transport ADD COLUMN new_driver_id UUID;
ALTER TABLE orders ADD COLUMN new_driver_id UUID;

--- Copy existing drivers to the Drivers table
WITH drivers AS (
    INSERT INTO driver( transport_id, old_id, name, phone, email)
        SELECT transport.id, transport.driver_id, users.name, users.phone, users.email
        FROM users JOIN transport ON transport.driver_id = users.id
        RETURNING id AS new_driver_id, transport_id as transport_id
)
--- Fill new driver ids with the ids of the newly created drivers
UPDATE transport SET new_driver_id = drivers.new_driver_id
FROM drivers
WHERE transport.id = drivers.transport_id;

WITH drivers AS (
    SELECT id, old_id from driver
)
UPDATE orders SET new_driver_id = drivers.id FROM drivers WHERE driver_id = old_id;

ALTER TABLE orders DROP CONSTRAINT FK_ORDERS_ON_DRIVER;
ALTER TABLE orders DROP COLUMN driver_id;
ALTER TABLE orders RENAME COLUMN new_driver_id TO driver_id;
ALTER TABLE orders
    ADD CONSTRAINT FK_ORDERS_ON_DRIVER FOREIGN KEY (driver_id) REFERENCES driver (id);

ALTER TABLE driver DROP COLUMN transport_id;
ALTER TABLE driver DROP COLUMN old_id;
ALTER TABLE transport DROP CONSTRAINT FK_AUTO_ON_DRIVER;

--- Delete drivers from the users table
DELETE FROM users
WHERE id IN (
    SELECT driver_id FROM transport
);

ALTER TABLE transport DROP COLUMN driver_id;
ALTER TABLE transport RENAME COLUMN new_driver_id TO driver_id;
ALTER TABLE transport ADD CONSTRAINT FK_TRANSPORT_ON_DRIVER FOREIGN KEY (driver_id) REFERENCES driver (id);

-- TRANSPORT add new columns
ALTER TABLE transport
    ADD COLUMN new_id UUID,
    ADD COLUMN transport_column_id UUID,
    ADD COLUMN main_transport_id UUID,
    ADD COLUMN park_number TEXT;

ALTER TABLE transport
    ADD CONSTRAINT FK_TRANSPORT_ON_TRANSPORT_COLUMN FOREIGN KEY (transport_column_id) REFERENCES transport_column (id);

-- Generate UUIDS for existing data
UPDATE transport SET new_id = gen_random_uuid() WHERE new_id IS NULL;

-- MERGED TRANSPORT

ALTER TABLE transport ADD COLUMN new_merged_to_id UUID;

WITH merged_transport AS (
    SELECT t1.id AS transport_id, t2.id AS old_merged_id, t2.new_id AS new_merged_id FROM
        transport t1 JOIN
        transport t2 ON t1.merged_to_id = t2.id
) UPDATE transport SET new_merged_to_id = merged_transport.new_merged_id
FROM merged_transport
WHERE transport.id = merged_transport.transport_id AND transport.merged_to_id = merged_transport.old_merged_id;

ALTER TABLE transport DROP CONSTRAINT FK_AUTO_ON_MERGED_TO;
ALTER TABLE transport DROP COLUMN merged_to_id;
ALTER TABLE transport RENAME COLUMN new_merged_to_id TO merged_to_id;

-- ORDER TO TRANSPORT ASSOCIATION
ALTER TABLE order_to_auto_assosiation RENAME TO order_to_transport_association;
ALTER TABLE order_to_transport_association ADD COLUMN transport_id UUID;

WITH ordtotrans AS (
    SELECT transport.new_id AS transport_new_id, order_to_transport_association.order_id AS order_id, transport.id AS transport_id
    FROM transport
             JOIN order_to_transport_association ON order_to_transport_association.auto_id = transport.id
)
UPDATE order_to_transport_association SET transport_id = ordtotrans.transport_new_id
FROM ordtotrans
WHERE order_to_transport_association.order_id = ordtotrans.order_id AND
    order_to_transport_association.auto_id = ordtotrans.transport_id;

ALTER TABLE order_to_transport_association DROP CONSTRAINT fk_ordtoautass_on_auto;
ALTER TABLE order_to_transport_association RENAME CONSTRAINT fk_ordtoautass_on_order TO fk_ordtotransass_on_order;
ALTER TABLE order_to_transport_association DROP COLUMN auto_id;

-- TASKS
ALTER TABLE tasks ADD COLUMN transport_id UUID;

WITH tasks_transport AS (
    SELECT transport.new_id as transport_new_id, tasks.id as task_id
    FROM transport
             JOIN tasks ON tasks.auto_id = transport.id
)
UPDATE tasks SET transport_id = tasks_transport.transport_new_id
FROM tasks_transport
WHERE tasks.id = tasks_transport.task_id;

ALTER TABLE tasks DROP CONSTRAINT FK_TASKS_ON_AUTO;
ALTER TABLE tasks DROP COLUMN auto_id;

--- UPDATE TRANSPORT PK

ALTER TABLE transport DROP CONSTRAINT pk_auto;
ALTER TABLE transport ADD CONSTRAINT pk_transport PRIMARY KEY (new_id);
ALTER TABLE transport DROP COLUMN id;
ALTER TABLE transport RENAME COLUMN new_id TO id;

ALTER TABLE order_to_transport_association ADD CONSTRAINT fk_ordtotransass_on_transport FOREIGN KEY (transport_id) REFERENCES transport (id);
ALTER TABLE transport
    ADD CONSTRAINT FK_TRANSPORT_TRAILER_ON_MAIN_TRANSPORT FOREIGN KEY (main_transport_id) REFERENCES transport (id);
ALTER TABLE transport ADD CONSTRAINT FK_TRANSPORT_ON_MERGED_TO  FOREIGN KEY (merged_to_id) REFERENCES transport (id);
ALTER TABLE tasks ADD CONSTRAINT FK_TASKS_ON_TRANSPORT FOREIGN KEY (transport_id) REFERENCES transport (id);

-- Add documents

CREATE TABLE document (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          owner_transport_id UUID,
                          owner_driver_id UUID,
                          filename VARCHAR(255),
                          filepath VARCHAR(500),
                          uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE document
    ADD CONSTRAINT FK_DOCUMENT_ON_TRANSPORT_OWNER FOREIGN KEY (owner_transport_id) REFERENCES transport (id);

ALTER TABLE document
    ADD CONSTRAINT FK_DOCUMENT_ON_DRIVER_OWNER FOREIGN KEY (owner_driver_id) REFERENCES driver (id);







