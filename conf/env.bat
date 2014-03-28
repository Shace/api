setx MEDIA_PATH "%~dp0\..\public\media\"
setx MEDIA_ROOT_URL "127.0.0.1:9000"
setx SECRET_KEY "8mtC^AGeQs`=ImcCTmGp^oKhjDr`IHs:n77isJy;Poko?KEHskTixTXCmkUltiEx"

setx DB_DRIVER "org.postgresql.Driver"
:: "com.mysql.jdbc.Driver"
setx DATABASE_URL "jdbc:postgresql://127.0.0.1/shace?user=postgres&password=postgres"
:: jdbc:mysql://localhost/shace?username=root
pause