# Conclusiones

## Resultados obtenidos

TODO: tabla resumen por suite, con la cantidad de casos, los fallidos en rojo, los fallidos en verde,
los defectos detectados y las fallas sembradas detectadas sobre el total.

## Efectividad de la estrategia

TODO: qué defectos detectó cada nivel y por qué no los habría detectado otro nivel. Algunos ejemplos:
- errores de mapeo, que solo aparecen con la base real;
- errores de transición, localizados gracias a los mocks;
- errores de sincronización, que solo aparecen con hilos reales.

## Limitaciones

TODO: lo que las suites no garantizan. Por ejemplo:
- la ausencia de condiciones de carrera no se demuestra con una ejecución en verde;
- los `assert` dependen de `-ea`;
- los supuestos tomados donde la especificación no define el comportamiento.

## Lecciones aprendidas

TODO
