<?php

include 'default.php';

$downloadUrl = "download.php";
$db = new db();
$app = new App($db);
$lastVer = $app->getLastVersion();

$count = $lastVer["downloaded"]+1;
$db->q("UPDATE {p}versionhistory SET downloaded = ? WHERE id = ?", $count, $lastVer["id"]); 

    
$file = "apks/".$lastVer["filename"];

$path = $file;//".".parse_url($file, PHP_URL_PATH);
if(!file_exists($path)) die("Failas nerastas.");

$logline = date("H:i:s")." ".$_SERVER["REMOTE_ADDR"];
$fp = fopen("logs/".date("Y-m-d").".log", "a+");
fwrite($fp, $logline."\n");
fclose($fp);

$extension = strtolower(pathinfo($path, PATHINFO_EXTENSION));

$filename = FIXED_FILENAME;
/*
$finfo = finfo_open(FILEINFO_MIME_TYPE); // return mime type ala mimetype extension
$mime = finfo_file($finfo, $path);
finfo_close($finfo);*/
$mime = "application/vnd.android.package-archive";

header("Content-Disposition: attachment; filename=\"$filename\"");   
header("Content-Type: application/force-download");
header("Content-Type: application/download");
header("Content-Type: ".$mime);

header("Content-Description: File Transfer");            
header("Content-Length: " . filesize($path));
flush(); // this doesn't really matter.
$fp = fopen($path, "r");
while (!feof($fp)) {
    echo fread($fp, 65536);
    flush(); // this is essential for large downloads
} 
fclose($fp);
?>