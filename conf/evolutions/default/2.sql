# Add image formats

# --- !Ups

INSERT INTO se_image_format
       (name, width, height, crop, type)
       VALUES 		('small', 270, 270, true, 0),
       	      		('medium', 1024, 1024, false, 0),
				   	('large', 3264, 2448, false, 0),
				   	('cover', 849, 313, true, 1);

INSERT INTO se_user
       (id, email, password, first_name, last_name, is_admin, inscription_date, lang)
       VALUES (0, 'admin@shace.io', '3ed4fd5119338b75e9fbcf7a1279f1f5b0025b64', 'Admin', '', true, Now(), 2);

# --- !Downs

DELETE FROM se_image_format;

DELETE FROM se_user WHERE email='admin@shace.io';
