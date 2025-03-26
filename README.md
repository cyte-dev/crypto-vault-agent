# Instrucciones
1. Este proyecto trabaja con JavaFX y Gradle. 

2. Instalar `dokan` en el computador con sistema operativo Windows. Concretamente, versión `1.4.0` disponible en: https://github.com/dokan-dev/dokany/releases?page=2

2. En la carpeta `libs` se encuentra el JAR asociado a dokan-java. En `build.gradle` ya se hace la implementación desde esta locación. En caso de que no lo reconozca, ejecutar:
```
mvn install:install-file -Dfile=libs/dokan-java-1.2.0-SNAPSHOT.jar -DgroupId=dev.dokan -DartifactId=dokan-java -Dversion=1.2.0-SNAPSHOT -Dpackaging=jar
```
La dependencia corresponde al siguiente repositorio: https://github.com/dokan-dev/dokan-java. Si se requiere, descargar el proyecto y generar el JAR mediante `gradle`. 

3. Acondicionar JavaFX de acuerdo a las instrucciones para el IDE específico.

# Funcionamiento

1. Al ejecutar la interfaz por primera vez, se abre un diálogo de configuraciones. Completar la IP del servidor y la ruta raíz (por ahora, no se tiene en cuenta). Se creará un archivo `.cfg` con la información corespondiente.

2. Ejecutar nuevamente y diligenciar el nombre de usuario y la contraseña con cualquier valor. 

3. En la interfaz principal, hacer click en `Conectar bóveda` y seleccionar una carpeta del disco. Si los elementos ahí contenidos no se hallan cifrados, los cifra (mantiene el contenido original, pero cambia la extensión a `.cv`). 

4. Se habilitará el botón `Desbloquear`, el cual toma los archivos originales cifrados, los descifra en memoria y los ubica en una unidad virtual. Es posible conectar múltiples bóvedas y desbloquearlas simultáneamente, para cada una se asigna un punto de montaje elegido al azar entre los disponibles.

> [!NOTE]  
> Es posible crear, editar y añadir nuevos archivos. Al eliminarlos, sale una advertencia, pero esta se puede ignorar y aún así se completa la acción. Ocurre lo mismo al crear archivos desde la unidad virtual, pero no si se copian y pegan desde el disco hacia ella. 

5. El botón `Bloquear` toma los archivos en la unidad virtual, los cifra y sobreescribe la ruta original. Además, borra el contenido en la unidad virtual. 

> [!WARNING]
> Por ahora, no se desmonta la unidad automáticamente, la única manera es detener el backend. Aún al detenerlo, se verán en el explorador de archivos, pero desconectadas. Aún así, será posible volver a montar la unidad virtual en esa ruta. 

9. Se pueden desconectar las bóvedas mediante el botón correspondiente.

10. Al ejecutar nuevamente la interfaz, las carpetas ya vinculadas se mantienen.
