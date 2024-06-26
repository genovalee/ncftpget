## 使用ncftpget從伺服器下載檔案至NT指定目錄
### 建立控制用table
```sql
create table FTPGETDEMO
(
  sqno       NUMBER(4) not null,
  remotepath VARCHAR2(100),
  remotefile VARCHAR2(50),
  localpath  VARCHAR2(100),
  dttm       VARCHAR2(13)
);
```
### 範例資料
```sql
insert into ftpgetdemo (SQNO, REMOTEPATH, REMOTEFILE, LOCALPATH, DTTM)
values (1, '/0001/test', '', 's:/mywork/source', '202207250916');

insert into ftpgetdemo (SQNO, REMOTEPATH, REMOTEFILE, LOCALPATH, DTTM)
values (2, '/0001/test', '*.*', 's:/mywork/source', '202207250916');

insert into ftpgetdemo (SQNO, REMOTEPATH, REMOTEFILE, LOCALPATH, DTTM)
values (3, '/0001/test', 'demo1.txt', 's:/mywork/source', '202207250916');
```

### 組裝執行指令
#### 給來源路徑下載樹狀目錄
```txt
S:\>ncftpget -u username -p password -d s:/mywork/log/20220725.log -R server s:/mywork/source /0001/test
```
#### 給來源檔案下載所有檔案
```txt
S:\>ncftpget -u username -p password -d s:/mywork/log/20220725.log server s:/mywork/source /0001/test/*.*
```
#### 給來源檔案下載指定檔案
```txt
S:\>ncftpget -u username -p password -d s:/mywork/log/20220725.log server s:/mywork/source /0001/test/TEST1.txt
```
### 屬性檔內容
```properties
# 資料庫連線設定
db.alias=host
db.username=schema
db.password=password

# FTP SERVER帳號密碼設定
ftp.host = server
ftp.username = xxxxxx
ftp.password = oooooo

# for test------------------
ftp.logdir = s:\\mywork\\log\\

# 備份檔案路徑設定
move.mk=Y
move.srcPath = S:\\mywork\\source\\
move.dstPath = S:\\mywork\\backup\\

# 執行store procedure註記，預設為Y，設定為N表示不執行store procedure
store.mk=Y
# 欲執行的store procedure
store.procedure=p_test

#清除超過30天的log檔，預設為Y
del.log.mk=Y

#remote path 註記，預設為Y(Y:遠端指定目錄下載，N:遠端指定檔案下載(須給檔名或用*.*))
use.remote.path=N
```
