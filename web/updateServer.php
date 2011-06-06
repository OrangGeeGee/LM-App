<?php
header('Cache-Control: no-cache, must-revalidate');
header('Expires: Mon, 26 Jul 1997 05:00:00 GMT');
//header('Content-type: text/json');

include 'default.php';

$downloadUrl = "download.php";
$app = new App();
$lastVer = $app->getLastVersion();

$response = array(
	"versionCode"=>$lastVer["versionCode"],
	"versionName"=>$lastVer["versionName"],
	"downloadUrl"=>$downloadUrl,
	"title"=>$lastVer["name"],
	"changelog"=>$lastVer["changelog"]
);

echo json_encode($response);


?>