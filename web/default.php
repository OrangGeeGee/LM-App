<?php

require('configs/definitions.php');
function autoload($className) {
    $folders = array("classes", "model", "controller");
    $done = false; $i = 0;
    while(!$done) {
        if(isset($folders[$i])) {
            $fn = ROOT."/".$folders[$i]."/".strtolower($className).".php";
            if(file_exists($fn)) {
                require_once $fn;
                $done = true;
            }      
        } else {
            $done = true;
            //die("No class '{$className}' found");
        }
        $i++;
    }
}
spl_autoload_register('autoload');
function dump() {
    $numargs = func_num_args();
    if($numargs>0) {
        $arg_list = func_get_args();
        echo "<div class='dump'>Dumped data: ";
        if($numargs > 1) {
            //if(!empty($text)) { $text</i> "; }
            echo "<a href='#' class='caption'>" . array_shift($arg_list) ."</a>";
            $c = "hidden";
        } else { $c = ""; }
        echo "<pre class='data $c'>";
        ob_start();
        foreach($arg_list as $arg) {
            var_dump($arg);
        }
        $dumped = htmlentities(ob_get_contents(), ENT_NOQUOTES, "UTF-8");
        ob_end_clean();
        echo $dumped."</pre></div>";
    }
}


/**
* Function shows info only to superadmin through message framework
* 
* @param string $str Message to display
*/
function debug($str) {
    //if(isset($_SESSION["superadmin"])&&$_SESSION["superadmin"]==true) Admin::message($str);        
    if(isset($_SESSION["superadmin"])&&$_SESSION["superadmin"]==true)
        if(!isset($_SESSION["debug"])) $_SESSION["debug"]=$str;
        else $_SESSION["debug"] .= $str;
}
function debugStart() {
    ob_start();
}
function debugEnd() {
    $content = ob_get_contents();
    ob_end_clean();
    debug($content);
}
function debugDump($mixed, $text="") {
    debugStart();
    dump($mixed, $text);
    debugEnd();
}


?>