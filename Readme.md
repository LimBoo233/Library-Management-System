## step 1  
import the library_db_backup.sql into mysql database.  
## step 2  
change the file hibernate.cfg.xml(src/main/resources)  
In the line name="connection.username"  change the username as your mysql username  
In the line name="connection.password"  change the password as your mysql password
## step 3
open project structure/Artifacts
delete all the artifacts remained in the Artifacts and use "+" to add a "Web Application: Exploded From Models" select LibrarySystem.
## step 4
Open "Run/Debug Configurations", select "tomcat", select "Deployment" delete remained artifact and add a new artifact use "+" then choose apply.
**Please choose a not occupied port in run configuration**
## step 5
Use run to start the project.