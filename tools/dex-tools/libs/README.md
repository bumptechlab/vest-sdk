# lib-dex-encrypt.jar

### 1. Summary
This is a library for encrypting file with AES and output to specified directory, the file structure as below:
* The first 2 bytes are 0xEEEE
* The next 44 bytes are a random 32 character base64 as the AES key followed by 1M bytes of AES encrypted bytes....
* 44 bytes are a random 32 characters of base64 as the AES key followed by the remaining bytes of the AES encrypted bytes
 
### 2. Project   
> `https://git.easycodesource.com/vest-int/dex-encrypt.git`

### 3. How to use it
(1) use -e option for encrypting   
> `java -jar lib-dex-encrypt.jar -e -i {inputFile} -o {outputFile}`

(2) use -d option for decrypting   
> `java -jar lib-dex-encrypt.jar -d -i {inputFile} -o {outputFile}`

- -e -encrypt: [need] operation for encrypting file
- -d -decrypt: [need] operation for decrypting file
- -i -input: [need] path of file path to be encrypted
- -o -output: [need] path of file for outputting encrypted file. default directory same as input file, and with file name end with '-encrypted'
- -h -help: show usage of lib-dex-encrypt.jar