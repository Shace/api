# Add image formats

# --- !Ups

INSERT INTO se_image_format
       (name, width, height, crop, type)
       VALUES 		('small', 270, 270, true, 0),
       	      		('medium', 1024, 1024, false, 0),
				   	('large', 3264, 2448, false, 0),
				   	('cover', 849, 313, true, 1),
				   	('profile', 200, 200, true, 2);

INSERT INTO se_user
       (id, email, password, first_name, last_name, is_admin, inscription_date, lang)
       VALUES (0, 'admin@shace.io', '3ed4fd5119338b75e9fbcf7a1279f1f5b0025b64', 'Admin', '', true, Now(), 2);

# --- !Downs

DELETE FROM se_image_format;

DELETE FROM se_access_token_event_relation WHERE accesstoken_id IN (SELECT token FROM se_access_token WHERE user_id=0);
DELETE FROM se_access_token WHERE user_id=0;
DELETE FROM se_beta_invitation WHERE original_user_id=0 OR created_user_id=0;
DELETE FROM se_event_user_relation WHERE user_id=0;
DELETE FROM se_tag WHERE user_id=0;
DELETE FROM se_media WHERE owner_id=0;
DELETE FROM se_feedback WHERE sender_id=0;
DELETE FROM se_user WHERE id=0;
