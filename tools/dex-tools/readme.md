#1.apk转aab技术实现
aab-build.sh的实现参考以下博文：
- https://blog.csdn.net/u012565335/article/details/123534703
- https://blog.csdn.net/Toast_yang/article/details/123891823

#2.问题汇总
##(1)安装aab，游戏没声音?
原因分析：主要是因为mp3文件从apk转aab过程中被压缩。\
解决方案：执行bundletool build-bundle时，使用--config配置mp3文件不可压缩。\
参考资料：
- https://github.com/google/bundletool/issues/203
- https://github.com/google/bundletool/blob/master/src/main/proto/config.proto#L7
- https://github.com/google/bundletool/issues/70