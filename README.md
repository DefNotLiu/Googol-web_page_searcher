# Web page searcher

Este projeto foi elaborado no contexto de sistemas discretos, no qual coordenámos e sincronizámos vários procressos usando a herança e java RMI. Foi integrado o uso de Spring Boot para responder os pedidos html pelo controller. Foi também integrado um API REST de Hacker News.

# Como correr o programa:

 - Executa-se o searcModule, o barrel e o downloader, respetivamente. 
 - Depois basta iniciar o cliente ou o cliente web(correr na diretoria webapp-demo a instrução ./mvnw spring-boot:run).
 - É preciso ter atenção ao endereço do IP do rmi, dado que este é atribuído a partir do nome e 
da placa de rede
da máquina. Basta alterar no ficheiro SearchModule na linha 98 para o nome da máquina local 
e na linha 106 escolher o índice da rede indicada.
