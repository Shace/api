# Add image formats

# --- !Ups

INSERT INTO se_image_format
       (name, width, height, crop)
       VALUES ('small', 270, 270, true),
       	      		('medium', 1024, 1024, false),
				   	 ('large', 3264, 2448, false);

INSERT INTO se_user
       (id, email, password, first_name, is_admin, inscription_date)
       VALUES (0, 'admin@shace.io', '3ed4fd5119338b75e9fbcf7a1279f1f5b0025b64', 'Admin', true, Now());

# --- !Downs

DELETE FROM se_image_format;

DELETE FROM se_user WHERE email='admin@shace.io';