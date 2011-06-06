<?php
/**
* Custom class for the interactions with mysql database through prepared statements
*/
class db extends mysqli {
    /**
    *  Prefix for all database tables
    *  @var string
    */
    public $prefix;
    /**
    *  Last mysql query ran
    *  @var string
    */
    public $lastquery = "";
    /**
    *  Rows affected by last query
    *  @var int
    */
    public $lastaffected = NULL;
    /**
    *  Rows inserted by last query
    *  @var int
    */
    public $lastinserted = NULL;
    /**
    *  Last mysql error
    *  @var string
    */    
    public $lasterror = "";

    
    /**
    * Initialize the class by connecting to the database
    * @param array $mysql Database connection information
    * @access public
    * @return void
    */
    public function __construct($mysql=NULL) {
        if(empty($mysql)) require './configs/mysql_info.php';
        ini_set("mysqli.reconnect", 1);
        $this->init();
        $this->options(MYSQLI_READ_DEFAULT_GROUP,"ft_min_word_len=3");
                                    
        $this->prefix = $mysql["prefix"];
        $this->real_connect($mysql["host"], $mysql["user"], $mysql["password"], $mysql["database"]);

        if(isset($this->connect_error)) {
            $this->lasterror = "MySQLi connect errno {$this->connect_errno}: {$this->connect_error}.";
            return false;
        }
        $this->set_charset("utf8");
        return true;
    }

    /**
    * Fetches data from a table
    * @param array $table Table name without the prefix
    * @param mixed $fields Fields to be fetched - 'array("field1", "field2")'
    * @param array $where Where clause - 'array("id"=>15, "name"=>array("LIKE"=>"Lop"))' is converted to 'WHERE id = 15 AND name LIKE `Lop`'
    * @param array $order Order clause - 'array("id"=>"asc")' is converted to 'ORDER BY id asc'
    * @param array $limit Limit the result set - 'array(0, 15)' is converted to 'LIMIT 0, 15'
    * @param array $group Group clause - 'array("field", "category")' is converted to 'GROUP BY field, category'
    * @access public
    * @return bool|array If data is fetched then an array with all the rows, else false
    */    
    public function fetch($table, $fields="*", $where="", $order="", $limit="", $group="") {
        $this->ping();
        $types = ""; $meta = array(&$types);
        if(is_array($fields)) { // laukai
            foreach($fields as $key=>$val)
                $fields[$key] = strpos($val, "(") || strpos($val, ".") ? $val : "`".$val."`";
            $f = implode(', ', $fields);
        } else {
            $f = "*";
        }
        $t = $this->handleTable($table);
        $w = $this->handleWhere($where, $meta, $table);
        if(!empty($w)) $w = " WHERE $w";
        
        if(is_array($order)&&!empty($order)) { // tvarka
            $tmparr = array();
            foreach($order as $key=>$val) {
                if(is_int($key)) $tmparr[] = "$val";
                else $tmparr[] = strpos($key, "(") || strpos($key, ".") ? "$key $val" : "`$key` $val";
            }
            $o = " ORDER BY ".implode(', ', $tmparr);
        } else {
            $o = "";
        }
        
        if(is_array($group)&&!empty($group)) { // grupavimas
            $tmparr = array();
            foreach($group as $key=>$val) {
                $tmparr[] = strpos($val, ".") ? "$val" : "`$val`";
            }
            $g = " GROUP BY ".implode(', ', $tmparr);
        } else {
            $g = "";
        }        
        
        if(is_array($limit)) {
            $l = " LIMIT {$limit[0]}, {$limit[1]}";
        } else {
            $l = "";
        }
        $query = "SELECT $f FROM {$t} {$w}{$g}{$o}{$l}";
        $this->lastquery = $query;
        if(!$p = $this->prepare($query)) {
            $this->lasterror = "MySQLi errno $this->errno: $this->error.";
            return false;
        }
        if(!empty($types)) call_user_func_array(array($p, 'bind_param'), $meta); 
        $p->execute();

        /* get resultset for metadata */
        $result = $p->result_metadata();
        $finfo = $result->fetch_fields();
        foreach ($finfo as $val) {
            $bind[] = &$a[$val->name];
        }
        $result->close();
        $p->store_result();

        call_user_func_array(array($p, 'bind_result'), $bind); 
        $data = NULL;
        while($p->fetch()) {
            foreach($a as $key=>$val) {
                $tmp[$key] = $val;
            }
            //if($p->num_rows==1) $data = $tmp; else //
            $data[] = $tmp;
        }

        $p->free_result();
        $p->close();
        
        return $data;
        
    }
    
    /**
    * Executes a query and returns select in a grouped manner
    * @param array $keys Name of the field to be used for grouping values
    * @param string $query Query to be executed
    * @access public
    * @return array Array with all returned rows grouped by elements in $keys array
    */  
    public function qKey() {
        $numargs = func_num_args();
        $arg_list = func_get_args();
        if($numargs==0) return false;
        $keys = array_shift($arg_list);
        $data = call_user_func_array(array($this, "q"), $arg_list);
        if(is_array($data)) {
            $output = array();
            if(!is_array($keys)) $keys = array($keys);
            foreach($data as $k=>$elem) {
                $pointer = &$output;
                $keysForShifting = $keys;
                while($key = array_shift($keysForShifting)) {
                    if(!is_array($pointer)) $pointer = array();
                    $pointer = &$pointer[$elem[$key]];
                }
                $pointer = $elem;
            }
        } else { $output = $data; }
        return $output;
    }
    public function q() {
        $this->lasterror = "";
        $this->lastaffected = NULL; 
        $this->lastinserted = NULL;
        $numargs = func_num_args();
        $arg_list = func_get_args();
        if($numargs==0) return false;
        
        $query = str_replace("{p}", $this->prefix, array_shift($arg_list));
        $this->lastquery = $query;
        
        $qmarks = substr_count($query, '?');
        if($numargs-1!=$qmarks) {
            $this->lasterror = "MySQLi error: Wrong amount of parameters specified. Needed $qmarks, got ".($numargs-1);
            return false;
        }
        
        if(!$p = $this->prepare($query)) {
            $this->lasterror = "MySQLi errno $this->errno: $this->error.";
            return false;
        }
        
        $types = "";
        $meta = array(&$types);
        foreach($arg_list as $i=>$arg) {
            $types .= $this->determineType($arg);
            $meta[] = &$arg_list[$i];
        }
        
        if(!empty($types)) call_user_func_array(array($p, 'bind_param'), $meta); 
        $p->execute();
        $this->lastaffected = $p->affected_rows;
        $this->lastinserted = $p->insert_id;
        /* get resultset for metadata */
        $result = $p->result_metadata();
        if($result) {
            $finfo = $result->fetch_fields();
            foreach ($finfo as $val) {
                $bind[] = &$a[$val->name];
            }
            $result->close();
            $p->store_result();

            call_user_func_array(array($p, 'bind_result'), $bind); 
            $data = NULL;
            while($p->fetch()) {
                foreach($a as $key=>$val) {
                    $tmp[$key] = $val;
                }
                //if($p->num_rows==1) $data = $tmp; else
                $data[] = $tmp;
            }
            
            $p->free_result();
                    
        } else {
            $data = true;
        }
        $p->close();
        return $data;
    }
    /**
    * Updates or inserts data depending on $where parameter
    * @param array $table Table name without the prefix
    * @param mixed $fields Fields with their values - 'array("name"=>"Jonas", "surname"=>"Petraitis")'
    * @param array $where If this is defined the function updates - 'array("id"=>array("<"=>15))' is converted to 'WHERE id < 15'
    * @access public
    * @return bool|int On success returns the id of the updated/inserted row, else false
    */    
    public function update($table, $fields, $where=NULL) {
        $this->ping();
        $types=""; $meta=array(&$types);
        $t = $this->handleTable($table);
        if(empty($where)) {
            $insert = true; $f = ""; $q="";
            foreach($fields as $field=>$value) {
                if($value=="NOW()") {
                    $fieldnames[] = "`".$field."`";
                    $qmarks[] = "NOW()";
                } else {
                    $fieldnames[] = "`".$field."`";
                    $qmarks[] = "?";
                    $type = $this->determineType($value);
                    //$meta[] = $type == "s" ? utf8_encode($fields[$field]) : $fields[$field];
                    $meta[] = $fields[$field];
                    $types .= $type;
                }                
            }
            $f = implode(', ', $fieldnames);
            $q = implode(', ', $qmarks);
            $query = "INSERT INTO $t ($f) VALUES ($q)";
        } else {
            $insert = false; $f = "";
            foreach($fields as $field=>$value) {
                $fieldnames[] = "`".$field."` = ?";
                $type = $this->determineType($value);
                //$meta[] = $type == "s" ? utf8_encode($fields[$field]) : $fields[$field];
                $meta[] = $fields[$field];
                $types .= $type;                
            }
            $f = implode(', ', $fieldnames);
            $w = $this->handleWhere($where, $meta);
            $query = "UPDATE $t SET $f WHERE $w";
        }
        $this->lastquery = $query;
        if(!$p = $this->prepare($query)) {
            $this->lasterror = "MySQLi errno $this->errno: $this->error.";
            return false;
        }
        if(!empty($types)) call_user_func_array(array($p, 'bind_param'), $meta); 
        $p->execute();
        $this->lastaffected = $p->affected_rows;
        if($insert) $this->lastinserted = $p->insert_id;
        $p->close();
        return true;
    }
    
    /**
    * Deletes rows from database
    * @param array $table Table name without the prefix
    * @param array $where Where clause - 'array("id"=>15,"type"=>array("<"=>3))' is converted to 'WHERE id = 15 AND type < `15`'
    * @access public
    * @return bool On success return true, else false
    */       
    public function delete($table, $where) {
        $this->ping();
        $types = ""; $meta = array(&$types);
        $w = $this->handleWhere($where, $meta);
        $t = $this->handleTable($table);
        $query = "DELETE FROM $t WHERE $w";
        $this->lastquery = $query;
        if(!$p = $this->prepare($query)) {
            $this->lasterror = "MySQLi errno $this->errno: $this->error.";
            return false;
        }
        if(!empty($types)) call_user_func_array(array($p, 'bind_param'), $meta); 
        $p->execute();
        $this->lastaffected = $p->affected_rows;
        $p->close();
        return true;
    }
    
    /**
    * Handles $table parameter from all the functions
    * @access protected
    * @return array digested data
    */      
    protected function handleTable($table) {
        if(!is_array($table))
            $str = $this->prefix.$table;
        else {
            $tmp = array();
            foreach($table as $tableName=>$acronym) {
                $tmp[] = $this->prefix.$tableName." AS ".$acronym;
            }
            $str = implode(", ", $tmp);
        }
        return $str;
    }
    /**
    * Handles $where parameters from all the functions
    * @access protected
    * @return array digested data
    */      
    protected function handleWhere(&$where, &$meta, $tables=array()) {
        if(empty($where)) {
            return NULL;
        } elseif(is_array($where)) { // filtravimas
            $tmparr = array();
            foreach($where as $key=>$value) {
                $quote = strpos($key, ".") || strpos($key, "=") ? "" : "`";
                if(is_array($value)) {
                    $tt = array();
                    foreach($value as $operand=>$val) {
                        if(is_array($val)) {
                            foreach($val as $k=>$v) {
                                $tmparr[] = "{$quote}$key{$quote} {$operand} ?";
                                $meta[0] .= $this->determineType($v);
                                $meta[] = &$where[$key][$operand][$k];                                
                            }
                        } else {
                            if(is_int($operand)) {
                                $tt[] = "{$quote}$key{$quote} = ?";
                                $meta[0] .= $this->determineType($val);
                                $meta[] = &$where[$key][$operand];
                            } else {
                                $tmparr[] = "{$quote}$key{$quote} {$operand} ?";
                                $meta[0] .= $this->determineType($val);
                                $meta[] = &$where[$key][$operand];                                
                            }
                        }                        
                    }
                    if(!empty($tt)) { $tmparr[] = "(".implode(" OR ", $tt).")"; }
                } else {
                    if(strpos($value, ".") && is_array($tables) && in_array(substr($value, 0, strpos($value, ".")),$tables))
                        $tmparr[] = "{$quote}$key{$quote} = $value";
                    else {
                        $tmparr[] = "{$quote}$key{$quote} = ?";
                        $meta[0] .= $this->determineType($value);
                        $meta[] = &$where[$key];
                    }
                }
            }
            return implode(" AND ", $tmparr);
        } else {
            return $where;
        }
    }

    /**
    * Determines the myslqi type of $var 
    * @param mixed $var
    * @access protected
    * @return string `i`, `d`, `s` depending on the type of $var
    */       
    protected function determineType($var) {
        if(is_float($var)) return "d";
        elseif(is_int($var)) return "i";
        else return "s";
    }
}

?>