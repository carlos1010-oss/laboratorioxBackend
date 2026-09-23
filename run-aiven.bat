@echo off
set SPRING_DATASOURCE_URL=jdbc:postgresql://pg-28ea2c3a-zonecontroldb.l.aivencloud.com:21773/defaultdb?sslmode=require
set SPRING_DATASOURCE_USERNAME=avnadmin
set SPRING_DATASOURCE_PASSWORD=AVNS_EldDSXtvLK_gtU0y1q4
.\mvnw.cmd -o spring-boot:run