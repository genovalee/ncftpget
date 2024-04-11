create table FTPGETDEMO
(
  sqno       NUMBER(4) not null,
  remotepath VARCHAR2(100),
  remotefile VARCHAR2(50),
  localpath  VARCHAR2(100),
  dttm       VARCHAR2(13)
);

insert into ftpgetdemo (SQNO, REMOTEPATH, REMOTEFILE, LOCALPATH, DTTM)
values (1, '/0001/test', '', 's:/fsg001/p20g01/cert', '202207250916');

insert into ftpgetdemo (SQNO, REMOTEPATH, REMOTEFILE, LOCALPATH, DTTM)
values (2, '/0001/test/demo1', '*.*', 's:/fsg001/p20g01/cert/demo1', '202207250916');

insert into ftpgetdemo (SQNO, REMOTEPATH, REMOTEFILE, LOCALPATH, DTTM)
values (3, '/0001/test/demo2', 'demo1.txt', 's:/fsg001/p20g01/cert/demo2', '202207250916');

#給來源路徑下載樹狀目錄，下載完成清除來源檔案
S:\fsg001\p20g01>ncftpget -u username -p password -d s:/fsg001/P20g01/log/20220725.log -DD -R TPFP01 s:/fsg001/p20g01/cert /0001/test
#給來源檔案下載檔案，下載完成清除來源檔案
S:\fsg001\p20g01>ncftpget -u username -p password -d s:/fsg001/P20g01/log/20220725.log -DD TPFP01 s:/fsg001/p20g01/cert /0001/test/*.*