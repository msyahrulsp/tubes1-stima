# tubes1-stima

Tugas Besar I IF2211 Strategi Algoritma <br>
Pemanfaatan Algoritma _Greedy_ dalam Aplikasi Permainan "Overdrive" <br>
Semester II 2021/2022

# Deskripsi Singkat
Overdrive adalah sebuah permainan yang mempertandingkan 2 bot mobil dalam ajang balapan. 
Adapun penggunaan strategi _greedy_ pada bot bertujuan untuk mendapatkan solusi yang optimum dan memenangkan permainan.
Strategi yang digunakan pada program ini adalah _greedy by speed_ dengan kombinasi. Strategi ini memfokuskan
agar bot mobil memiliki kecepatan yang optimal dan dapat mencapai garis finish lebih dahulu. 
Adapun pada strategi ini juga mengkombinasikan dengan strategi _greedy by damage_ untuk menyerang musuh dan
_greedy by score_ untuk menjaga mobil agar selalu dalam kondisi optimum. 

# Requirements Program
1. Java (minimal Java 8): https://www.oracle.com/java/technologies/javase/javasejdk8-downloads.html
2. Pastikan konfigurasi JAVA_HOME di PATH sudah benar, untuk Windows 10 dapat membuka 
environment variables dan membuat system variables JAVA_HOME yang berisi directory tempat JDK berada
3. Apache Maven untuk build Jar executable file dengan dependency
4. IntellIJ untuk build Jar executable file dengan dependency 

# Cara Menggunakan Program
1. Download latest release starter-pack.zip dari tautan https://github.com/EntelectChallenge/2020-Overdrive/releases/tag/2020.3.4
2. Ekstrak starter-pack.zip.
3. Download dan ekstrak zip dari repository ini ke folder tempat starter pack.
4. Apabila ingin melakukan build ulang file src, dapat menggunakan Apache Maven. (Dapat dibantu melalui dengan IntellIJ atau melalui VSCode)
4. Ubah isi file game-runner-config.json pada bagian player-a atau di player-b dengan directory tempat file .jar dan bot.json dari repository berada. 
File .jar dan bot.json ada di folder bin di dalam repository ini. 
5. Ubah bagian player lainnya di game-runner-config.json dengan directory bot lawan.
6. Jalankan file run.bat, akan muncul tampilan informasi untuk tiap ronde permainan.
7. Gunakan replayer untuk mendapatkan visualisasi yang lebih baik. Dapat diakses pada tautan https://entelect-replay.raezor.co.za/

# Author
Kelompok 46 - TheDoctor

| NIM      | NAMA                       |
|----------|----------------------------|
| 13520003 | Dzaky Fattan Rizqullah     | 
| 13520139 | Fachry Dennis Heraldi      | 
| 13520161 | M Syahrul Surya Putra      | 