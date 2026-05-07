UPDATE masters
SET employment_status = CASE
    WHEN EXISTS (
        SELECT 1
        FROM orders
        WHERE orders.master_id = masters.id
          AND orders.status IN ('CREATED', 'IN_PROGRESS')
    ) THEN 'BUSY'
    ELSE 'FREE'
END;
