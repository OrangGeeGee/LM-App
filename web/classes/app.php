<?php

class App {
    private $db;
    
    public function __construct($db = null) {
        if($db==null) {
            $this->db = new db();
        } else {
            $this->db = $db;
        }
    }
    
    public function getLastVersion() {
        list($ver) = $this->db->q("SELECT * FROM {p}versionhistory ORDER BY versionCode DESC LIMIT 0,1");
        return $ver;
    }
    
}
?>
