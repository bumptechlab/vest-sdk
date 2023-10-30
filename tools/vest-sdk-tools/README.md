
1. this is a lib for encrypting config file using in vest-sdk, project repository as below:   
> `https://git.easycodesource.com/vest-int/vest-sdk-encrypt`

2. How to use it.

use -e option for encrypting
> `java -jar lib-vest-sdk-encrypt.jar -e -i {inputFile} -o {outputFile}`

use -d option for decrypting
> `java -jar lib-vest-sdk-encrypt.jar -d -i {inputFile} -o {outputFile}`

- -e -encrypt: [need] operation for encrypting file
- -d -encrypt: [need] operation for decrypting file
- -i -input: [need] path of file path to be encrypted
- -o -output: [optional] path of file for outputting encrypted file. default directory same as input file, and with file name end with '-encrypted'
- -h -help: show usage of lib-vest-sdk-encrypt.jar