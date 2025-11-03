## Getting Started

⚙️ Como executar

Baixe o Gson JAR
 e coloque em /lib.

Compile todos os arquivos:

javac -cp lib/gson-2.10.1.jar src/*.java -d bin


Rode o servidor:

java -cp "bin;lib/gson-2.10.1.jar" Servidor


Em outro terminal, rode um eleitor:

java -cp "bin;lib/gson-2.10.1.jar" ClienteVotante


E outro terminal para o administrador:

java -cp "bin;lib/gson-2.10.1.jar" ClienteAdmin
