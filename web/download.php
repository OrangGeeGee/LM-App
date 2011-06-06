<?php

include 'default.php';

$downloadUrl = "download.php";
$db = new db();
$app = new App($db);
$lastVer = $app->getLastVersion();

$location = "apks/".urlencode($lastVer["fileName"]);
$count = $lastVer["downloaded"]+1;
$db->q("UPDATE {p}versionhistory SET downloaded = ? WHERE id = ?", $count, $lastVer["id"]); 
Header("Location: ".$location);
?>