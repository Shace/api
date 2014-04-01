# Add image formats

# --- !Ups

INSERT INTO se_image_format
       (name, width, height, crop)
       VALUES ('small', 270, 270, true),
       	      		('medium', 1024, 1024, false),
				   	 ('large', 3264, 2448, false);

# --- !Downs

DELETE FROM se_image_format;