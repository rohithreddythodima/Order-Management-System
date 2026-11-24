DO $seed$
DECLARE
    v_menu_ids BIGINT[];
    v_item_names TEXT[] := ARRAY[
        'Poutine',
        'Montreal Smoked Meat Sandwich',
        'Quebec Tourtiere',
        'Maple Glazed Salmon',
        'Peameal Bacon Sandwich',
        'Montreal Bagel',
        'Nanaimo Bar',
        'Butter Tart',
        'Bannock Taco',
        'Maple Pecan Pancakes',
        'Canadian Cheddar Burger',
        'Blueberry Grunt'
    ];
    v_customer_names TEXT[] := ARRAY[
        'Alex Morgan',
        'Jordan Taylor',
        'Casey Lee',
        'Avery Martin',
        'Riley Chen',
        'Sam Patel'
    ];
    v_day_offset INTEGER;
    v_order_index INTEGER;
    v_order_sequence INTEGER;
    v_item_sequence INTEGER;
    v_line_count INTEGER;
    v_menu_id BIGINT;
    v_menu_name TEXT;
    v_unit_price NUMERIC(10, 2);
    v_quantity INTEGER;
    v_order_id BIGINT;
    v_order_total NUMERIC(10, 2);
    v_order_time TIMESTAMP;
    v_customer_name TEXT;
BEGIN
    INSERT INTO menu_items (name, description, price, image_url, available, created_at)
    SELECT sample.name, sample.description, sample.price, sample.image_url, TRUE, CURRENT_TIMESTAMP
    FROM (VALUES
        ('Poutine', 'Crispy fries, Quebec cheese curds, and rich gravy.', 12.50::NUMERIC(10, 2), '/images/poutine.jpg'),
        ('Montreal Smoked Meat Sandwich', 'Slow-smoked beef piled high on rye with mustard.', 18.00::NUMERIC(10, 2), '/images/smoked-meat-sandwich.jpg'),
        ('Quebec Tourtiere', 'A warm, savory spiced meat pie with flaky pastry.', 16.50::NUMERIC(10, 2), '/images/tourtiere.jpg'),
        ('Maple Glazed Salmon', 'Roasted salmon with a sweet Canadian maple glaze.', 23.00::NUMERIC(10, 2), '/images/maple-salmon.jpg'),
        ('Peameal Bacon Sandwich', 'Canadian peameal bacon on a soft bakery bun.', 15.00::NUMERIC(10, 2), '/images/peameal-sandwich.jpg'),
        ('Montreal Bagel', 'Hand-rolled, wood-fired sesame bagel with cream cheese.', 7.50::NUMERIC(10, 2), '/images/montreal-bagel.jpg'),
        ('Nanaimo Bar', 'Classic no-bake chocolate, custard, and coconut square.', 6.00::NUMERIC(10, 2), '/images/nanaimo-bar.jpg'),
        ('Butter Tart', 'Flaky Canadian pastry with a rich, gooey maple filling.', 5.50::NUMERIC(10, 2), '/images/butter-tart.jpg'),
        ('Bannock Taco', 'Fresh bannock topped with spiced beans and crisp slaw.', 14.00::NUMERIC(10, 2), '/images/bannock-taco.jpg'),
        ('Maple Pecan Pancakes', 'Fluffy pancakes with toasted pecans and maple syrup.', 13.50::NUMERIC(10, 2), '/images/maple-pancakes.jpg'),
        ('Canadian Cheddar Burger', 'Grilled beef, aged cheddar, lettuce, and house sauce.', 19.00::NUMERIC(10, 2), '/images/cheddar-burger.jpg'),
        ('Blueberry Grunt', 'Warm wild blueberry cobbler with vanilla cream.', 8.00::NUMERIC(10, 2), '/images/blueberry-grunt.jpg'),
        ('Crispy Fish and Chips', 'Beer-battered haddock with fries and house tartar sauce.', 18.50::NUMERIC(10, 2), '/images/fish-and-chips.jpg'),
        ('Creamy Split Pea Soup', 'Slow-simmered split peas with herbs and toasted bread.', 9.00::NUMERIC(10, 2), '/images/pea-soup.jpg'),
        ('Canadian Apple Pie', 'Warm apple pie with cinnamon and vanilla cream.', 8.50::NUMERIC(10, 2), '/images/canadian-apple-pie.jpg'),
        ('Wild Berry Cheesecake', 'Creamy cheesecake topped with seasonal berries.', 9.50::NUMERIC(10, 2), '/images/berry-cheesecake.jpg'),
        ('Sugar Shack Donuts', 'Freshly fried dough tossed in maple sugar.', 7.00::NUMERIC(10, 2), '/images/sugar-donuts.jpg'),
        ('Roasted Tomato Soup', 'Roasted tomatoes blended with basil and cream.', 8.50::NUMERIC(10, 2), '/images/pea-soup.jpg'),
        ('Crispy Cauliflower Bites', 'Golden cauliflower with smoky paprika dip.', 10.00::NUMERIC(10, 2), '/images/fish-and-chips.jpg'),
        ('Butternut Squash Risotto', 'Creamy arborio rice with roasted squash and sage.', 17.50::NUMERIC(10, 2), '/images/pea-soup.jpg'),
        ('Grilled Chicken Sandwich', 'Herb chicken, crisp lettuce, and garlic aioli on a bun.', 16.00::NUMERIC(10, 2), '/images/peameal-sandwich.jpg'),
        ('Maple BBQ Chicken Wings', 'Chicken wings glazed with tangy maple barbecue sauce.', 14.50::NUMERIC(10, 2), '/images/cheddar-burger.jpg'),
        ('Loaded Cheese Fries', 'Crispy fries topped with cheddar, scallions, and sour cream.', 11.00::NUMERIC(10, 2), '/images/poutine.jpg'),
        ('Wild Mushroom Flatbread', 'Roasted mushrooms, mozzarella, and fresh thyme.', 15.50::NUMERIC(10, 2), '/images/tourtiere.jpg'),
        ('Beef and Barley Stew', 'Slow-braised beef with barley and root vegetables.', 18.00::NUMERIC(10, 2), '/images/pea-soup.jpg'),
        ('Maple Glazed Carrots', 'Roasted carrots finished with maple and fresh parsley.', 8.00::NUMERIC(10, 2), '/images/maple-salmon.jpg'),
        ('Crispy Onion Rings', 'Hand-breaded onion rings with house dipping sauce.', 9.00::NUMERIC(10, 2), '/images/fish-and-chips.jpg'),
        ('Chocolate Fudge Brownie', 'Rich chocolate brownie served with vanilla cream.', 8.00::NUMERIC(10, 2), '/images/nanaimo-bar.jpg'),
        ('Lemon Blueberry Tart', 'Buttery tart filled with lemon custard and blueberries.', 8.50::NUMERIC(10, 2), '/images/blueberry-grunt.jpg'),
        ('Vanilla Bean Pudding', 'Silky vanilla pudding with a crisp sugar top.', 7.00::NUMERIC(10, 2), '/images/berry-cheesecake.jpg'),
        ('Maple Cream Milkshake', 'Vanilla ice cream blended with Canadian maple syrup.', 7.50::NUMERIC(10, 2), '/images/sugar-donuts.jpg'),
        ('Fresh Berry Lemonade', 'House lemonade shaken with seasonal berries.', 5.50::NUMERIC(10, 2), '/images/blueberry-grunt.jpg'),
        ('Coffee and Butter Tart', 'Freshly brewed coffee paired with a classic butter tart.', 9.00::NUMERIC(10, 2), '/images/butter-tart.jpg')
    ) AS sample(name, description, price, image_url)
    WHERE NOT EXISTS (
        SELECT 1 FROM menu_items existing WHERE existing.name = sample.name
    );

    UPDATE menu_items
    SET image_url = CASE name
        WHEN 'Roasted Tomato Soup' THEN '/images/pea-soup.jpg'
        WHEN 'Crispy Cauliflower Bites' THEN '/images/fish-and-chips.jpg'
        WHEN 'Butternut Squash Risotto' THEN '/images/pea-soup.jpg'
        WHEN 'Grilled Chicken Sandwich' THEN '/images/peameal-sandwich.jpg'
        WHEN 'Maple BBQ Chicken Wings' THEN '/images/cheddar-burger.jpg'
        WHEN 'Loaded Cheese Fries' THEN '/images/poutine.jpg'
        WHEN 'Wild Mushroom Flatbread' THEN '/images/tourtiere.jpg'
        WHEN 'Beef and Barley Stew' THEN '/images/pea-soup.jpg'
        WHEN 'Maple Glazed Carrots' THEN '/images/maple-salmon.jpg'
        WHEN 'Crispy Onion Rings' THEN '/images/fish-and-chips.jpg'
        WHEN 'Chocolate Fudge Brownie' THEN '/images/nanaimo-bar.jpg'
        WHEN 'Lemon Blueberry Tart' THEN '/images/blueberry-grunt.jpg'
        WHEN 'Vanilla Bean Pudding' THEN '/images/berry-cheesecake.jpg'
        WHEN 'Maple Cream Milkshake' THEN '/images/sugar-donuts.jpg'
        WHEN 'Fresh Berry Lemonade' THEN '/images/blueberry-grunt.jpg'
        WHEN 'Coffee and Butter Tart' THEN '/images/butter-tart.jpg'
    END
    WHERE name IN (
        'Roasted Tomato Soup', 'Crispy Cauliflower Bites', 'Butternut Squash Risotto',
        'Grilled Chicken Sandwich', 'Maple BBQ Chicken Wings', 'Loaded Cheese Fries',
        'Wild Mushroom Flatbread', 'Beef and Barley Stew', 'Maple Glazed Carrots',
        'Crispy Onion Rings', 'Chocolate Fudge Brownie', 'Lemon Blueberry Tart',
                'Vanilla Bean Pudding', 'Maple Cream Milkshake', 'Fresh Berry Lemonade',
                'Coffee and Butter Tart'
    )
      AND image_url IS NULL;

        DELETE FROM menu_items
        WHERE name IN ('Baked Mac and Cheese', 'Garden Harvest Salad');

    UPDATE menu_items
    SET image_url = CASE name
        WHEN 'Poutine' THEN '/images/poutine.jpg'
        WHEN 'Montreal Smoked Meat Sandwich' THEN '/images/smoked-meat-sandwich.jpg'
        WHEN 'Quebec Tourtiere' THEN '/images/tourtiere.jpg'
        WHEN 'Maple Glazed Salmon' THEN '/images/maple-salmon.jpg'
        WHEN 'Peameal Bacon Sandwich' THEN '/images/peameal-sandwich.jpg'
        WHEN 'Montreal Bagel' THEN '/images/montreal-bagel.jpg'
        WHEN 'Nanaimo Bar' THEN '/images/nanaimo-bar.jpg'
        WHEN 'Butter Tart' THEN '/images/butter-tart.jpg'
        WHEN 'Bannock Taco' THEN '/images/bannock-taco.jpg'
        WHEN 'Maple Pecan Pancakes' THEN '/images/maple-pancakes.jpg'
        WHEN 'Canadian Cheddar Burger' THEN '/images/cheddar-burger.jpg'
        WHEN 'Blueberry Grunt' THEN '/images/blueberry-grunt.jpg'
    END
    WHERE name = ANY(v_item_names)
      AND image_url LIKE 'https://images.unsplash.com/%';

    DELETE FROM order_line_items
    WHERE order_id IN (
        SELECT id
        FROM customer_orders
        WHERE customer_name LIKE 'Sample%#0-%'
          AND created_at::DATE = CURRENT_DATE
    );

    DELETE FROM customer_orders
    WHERE customer_name LIKE 'Sample%#0-%'
      AND created_at::DATE = CURRENT_DATE;

    SELECT array_agg(id ORDER BY id)
    INTO v_menu_ids
    FROM menu_items
    WHERE name = ANY(v_item_names);

    IF coalesce(array_length(v_menu_ids, 1), 0) = 0 THEN
        RAISE EXCEPTION 'No sample menu items were available to create demo orders.';
    END IF;

    FOR v_day_offset IN 1..30 LOOP
        v_order_index := 3;

        FOR v_order_sequence IN 1..v_order_index LOOP
            v_customer_name := 'Sample · '
                || v_customer_names[((v_order_sequence - 1) % array_length(v_customer_names, 1)) + 1]
                || ' #' || v_day_offset || '-' || v_order_sequence;

            IF NOT EXISTS (
                SELECT 1
                FROM customer_orders existing
                WHERE existing.customer_name = v_customer_name
                  AND existing.created_at::DATE = CURRENT_DATE - v_day_offset
            ) THEN
                v_order_time := CURRENT_DATE - v_day_offset + TIME '11:00'
                    + make_interval(hours => floor(random() * 9)::INTEGER,
                                    mins => floor(random() * 60)::INTEGER);
                v_line_count := 1 + floor(random() * 3)::INTEGER;
                v_order_total := 0;

                INSERT INTO customer_orders (customer_name, status, total, created_at)
                VALUES (v_customer_name, 'COMPLETED', 0, v_order_time)
                RETURNING id INTO v_order_id;

                FOR v_item_sequence IN 1..v_line_count LOOP
                    v_menu_id := v_menu_ids[
                        floor(random() * array_length(v_menu_ids, 1))::INTEGER + 1
                    ];
                    v_quantity := 1 + floor(random() * 3)::INTEGER;

                    SELECT name, price INTO v_menu_name, v_unit_price
                    FROM menu_items WHERE id = v_menu_id;

                    INSERT INTO order_line_items (
                        order_id, menu_item_id, item_name, unit_price, quantity, line_total
                    ) VALUES (
                        v_order_id,
                        v_menu_id,
                        v_menu_name,
                        v_unit_price,
                        v_quantity,
                        v_unit_price * v_quantity
                    );

                    v_order_total := v_order_total + v_unit_price * v_quantity;
                END LOOP;

                UPDATE customer_orders SET total = v_order_total WHERE id = v_order_id;
            END IF;
        END LOOP;
    END LOOP;
END;
$seed$;
