# Instrucciones
1. Este proyecto trabaja con JavaFX y Gradle. 

2. Instalar `dokan` en el computador con sistema operativo Windows. Concretamente, versión `1.4.0` disponible en: https://github.com/dokan-dev/dokany/releases?page=2

2. En la carpeta `libs` se encuentra el JAR asociado a dokan-java. En `build.gradle` ya se hace la implementación desde esta locación. En caso de que no lo reconozca, ejecutar:
```
mvn install:install-file -Dfile=libs/dokan-java-1.2.0-SNAPSHOT.jar -DgroupId=dev.dokan -DartifactId=dokan-java -Dversion=1.2.0-SNAPSHOT -Dpackaging=jar
```
La dependencia corresponde al siguiente repositorio: https://github.com/dokan-dev/dokan-java. Si se requiere, descargar el proyecto y generar el JAR mediante `gradle`. 

3. Acondicionar JavaFX de acuerdo a las instrucciones para el IDE específico.
