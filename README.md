# sincronizacao-receita

This project is a fictional service for sending bank account information from a bank stored in a CSV file to another service of the Federal Revenue for validation of these accounts.

****
#### Functional requirements:
0. Create a standalone SprintBoot application.
1. Process an input CSV file with the format below.
2. Send the update to the Revenue through the service (simulated by the RecipeService class).
3. Returns a file with the result of sending the recipe update. Same format adding the result in a new column.

#### CSV example:  
Input:  
agencia;conta;saldo;status  
0101;12225-6;100,00;P  
0101;12226-8;3200.50;A  
3202;40011-1;-35,12;I  
3202;54001-2;0,00;P  
3202;00321-2;34500,00;B  

Output:  
agencia;conta;saldo;status;resultado  
3202;54001-2;0,00;P;true  
0101;12225-6;100,00;P;true  
3202;00321-2;34500,00;B;true  
3202;40011-1;-35,12;I;true  
0101;12226-8;3200.50;A;true  

### How to use:
- Download the jar file provided in the [releases page](https://github.com/IgorGMoraes/sincronizacao-receita/releases/tag/v1.0) 
- Place your CSV file in the same directory
- Then run: `java - jar sincronizacao-receita-1.0.jar <CSV_file_name>`

***
#### Technologies used:
- Java
- Spring Boot
- Spring Batch (running the job asynchronously)
