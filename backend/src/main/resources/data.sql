INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Poutine', 'Crispy fries, Quebec cheese curds, and rich gravy.', 12.50, '/images/poutine.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Poutine');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Montreal Smoked Meat Sandwich', 'Slow-smoked beef piled high on rye with mustard.', 18.00, '/images/smoked-meat-sandwich.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Montreal Smoked Meat Sandwich');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Quebec Tourtiere', 'A warm, savory spiced meat pie with flaky pastry.', 16.50, '/images/tourtiere.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Quebec Tourtiere');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Maple Glazed Salmon', 'Roasted salmon with a sweet Canadian maple glaze.', 23.00, '/images/maple-salmon.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Maple Glazed Salmon');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Peameal Bacon Sandwich', 'Canadian peameal bacon on a soft bakery bun.', 15.00, '/images/peameal-sandwich.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Peameal Bacon Sandwich');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Montreal Bagel', 'Hand-rolled, wood-fired sesame bagel with cream cheese.', 7.50, '/images/montreal-bagel.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Montreal Bagel');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Nanaimo Bar', 'Classic no-bake chocolate, custard, and coconut square.', 6.00, '/images/nanaimo-bar.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Nanaimo Bar');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Butter Tart', 'Flaky Canadian pastry with a rich, gooey maple filling.', 5.50, '/images/butter-tart.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Butter Tart');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Bannock Taco', 'Fresh bannock topped with spiced beans and crisp slaw.', 14.00, '/images/bannock-taco.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Bannock Taco');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Maple Pecan Pancakes', 'Fluffy pancakes with toasted pecans and maple syrup.', 13.50, '/images/maple-pancakes.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Maple Pecan Pancakes');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Canadian Cheddar Burger', 'Grilled beef, aged cheddar, lettuce, and house sauce.', 19.00, '/images/cheddar-burger.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Canadian Cheddar Burger');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Blueberry Grunt', 'Warm wild blueberry cobbler with vanilla cream.', 8.00, '/images/blueberry-grunt.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Blueberry Grunt');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Crispy Fish and Chips', 'Beer-battered haddock with fries and house tartar sauce.', 18.50, '/images/fish-and-chips.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Crispy Fish and Chips');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Creamy Split Pea Soup', 'Slow-simmered split peas with herbs and toasted bread.', 9.00, '/images/pea-soup.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Creamy Split Pea Soup');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Canadian Apple Pie', 'Warm apple pie with cinnamon and vanilla cream.', 8.50, '/images/canadian-apple-pie.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Canadian Apple Pie');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Wild Berry Cheesecake', 'Creamy cheesecake topped with seasonal berries.', 9.50, '/images/berry-cheesecake.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Wild Berry Cheesecake');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Sugar Shack Donuts', 'Freshly fried dough tossed in maple sugar.', 7.00, '/images/sugar-donuts.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Sugar Shack Donuts');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Roasted Tomato Soup', 'Roasted tomatoes blended with basil and cream.', 8.50, '/images/pea-soup.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Roasted Tomato Soup');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Crispy Cauliflower Bites', 'Golden cauliflower with smoky paprika dip.', 10.00, '/images/fish-and-chips.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Crispy Cauliflower Bites');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Butternut Squash Risotto', 'Creamy arborio rice with roasted squash and sage.', 17.50, '/images/pea-soup.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Butternut Squash Risotto');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Grilled Chicken Sandwich', 'Herb chicken, crisp lettuce, and garlic aioli on a bun.', 16.00, '/images/peameal-sandwich.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Grilled Chicken Sandwich');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Maple BBQ Chicken Wings', 'Chicken wings glazed with tangy maple barbecue sauce.', 14.50, '/images/cheddar-burger.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Maple BBQ Chicken Wings');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Loaded Cheese Fries', 'Crispy fries topped with cheddar, scallions, and sour cream.', 11.00, '/images/poutine.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Loaded Cheese Fries');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Wild Mushroom Flatbread', 'Roasted mushrooms, mozzarella, and fresh thyme.', 15.50, '/images/tourtiere.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Wild Mushroom Flatbread');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Beef and Barley Stew', 'Slow-braised beef with barley and root vegetables.', 18.00, '/images/pea-soup.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Beef and Barley Stew');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Maple Glazed Carrots', 'Roasted carrots finished with maple and fresh parsley.', 8.00, '/images/maple-salmon.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Maple Glazed Carrots');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Crispy Onion Rings', 'Hand-breaded onion rings with house dipping sauce.', 9.00, '/images/fish-and-chips.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Crispy Onion Rings');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Chocolate Fudge Brownie', 'Rich chocolate brownie served with vanilla cream.', 8.00, '/images/nanaimo-bar.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Chocolate Fudge Brownie');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Lemon Blueberry Tart', 'Buttery tart filled with lemon custard and blueberries.', 8.50, '/images/blueberry-grunt.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Lemon Blueberry Tart');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Vanilla Bean Pudding', 'Silky vanilla pudding with a crisp sugar top.', 7.00, '/images/berry-cheesecake.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Vanilla Bean Pudding');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Maple Cream Milkshake', 'Vanilla ice cream blended with Canadian maple syrup.', 7.50, '/images/sugar-donuts.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Maple Cream Milkshake');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Fresh Berry Lemonade', 'House lemonade shaken with seasonal berries.', 5.50, '/images/blueberry-grunt.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Fresh Berry Lemonade');

INSERT INTO menu_items (name, description, price, image_url, available, created_at)
SELECT 'Coffee and Butter Tart', 'Freshly brewed coffee paired with a classic butter tart.', 9.00, '/images/butter-tart.jpg', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Coffee and Butter Tart');

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
	AND (image_url IS NULL OR image_url = '');

DELETE FROM menu_items
WHERE name IN ('Baked Mac and Cheese', 'Garden Harvest Salad');