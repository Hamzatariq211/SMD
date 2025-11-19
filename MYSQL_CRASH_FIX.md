# MySQL XAMPP Crash Fix Guide

## Problem
MySQL in XAMPP keeps stopping unexpectedly with error:
```
Status change detected: stopped
Error: MySQL shutdown unexpectedly.
This may be due to a blocked port, missing dependencies, 
improper privileges, a crash, or a shutdown by another method.
```

## Common Causes & Solutions

### Solution 1: Port Conflict (Most Common)
MySQL default port 3306 might be used by another service.

**Steps to Fix:**
1. Open XAMPP Control Panel as Administrator (Right-click → Run as administrator)
2. Click "Config" button next to MySQL → my.ini
3. Find the line with `port=3306`
4. Change it to `port=3307` (or another unused port)
5. Also find `[client]` section and change port there too:
   ```
   [client]
   port=3307
   ```
6. Save the file
7. Update your PHP database config:
   - Open: `E:\Mobile dev Projects\i210396\instagram_api\config\Database.php`
   - Change connection to use new port (see fix below)

### Solution 2: Corrupted MySQL Data Files

**Steps to Fix:**
1. Stop MySQL in XAMPP
2. Navigate to: `C:\xampp\mysql\data\` (or your XAMPP installation folder)
3. Rename `mysql` folder to `mysql_backup`
4. Copy the `mysql` folder from `C:\xampp\mysql\backup\` to `C:\xampp\mysql\data\`
5. Start MySQL again
6. Re-import your database

### Solution 3: Missing InnoDB Files

**Steps to Fix:**
1. Stop MySQL
2. Go to `C:\xampp\mysql\data\`
3. Delete these files if they exist:
   - `ib_logfile0`
   - `ib_logfile1`
   - `ibdata1`
4. Start MySQL (it will recreate these files)
5. Re-import your database

### Solution 4: Incorrect Shutdown Previously

**Steps to Fix:**
1. Open Task Manager (Ctrl+Shift+Esc)
2. Find any running `mysqld.exe` processes
3. End all MySQL processes
4. Try starting MySQL in XAMPP again

### Solution 5: Permission Issues

**Steps to Fix:**
1. Right-click on XAMPP Control Panel
2. Select "Run as Administrator"
3. Try starting MySQL
4. If it works, always run XAMPP as administrator

### Solution 6: Check Error Logs

**Steps to Check Logs:**
1. In XAMPP Control Panel, click "Logs" button next to MySQL
2. Look for the actual error message
3. Common errors and fixes:
   - **"Can't start server: Bind on TCP/IP port"** → Port conflict (use Solution 1)
   - **"Table doesn't exist"** → Corrupted data (use Solution 2)
   - **"InnoDB: unable to lock"** → Delete InnoDB files (use Solution 3)

## Quick Fix - Step by Step

### RECOMMENDED: Try this first

1. **Stop all MySQL services:**
   - Open Command Prompt as Administrator
   - Run: `net stop mysql`
   - Close any MySQL Workbench or phpMyAdmin tabs

2. **Clear MySQL locks:**
   - Navigate to: `C:\xampp\mysql\data\`
   - Delete any `.pid` files you see (like `hostname.pid`)

3. **Start MySQL in XAMPP:**
   - Open XAMPP Control Panel as Administrator
   - Click Start for MySQL

4. **If still failing, check port:**
   - Open Command Prompt as Administrator
   - Run: `netstat -ano | findstr :3306`
   - If you see output, another service is using port 3306
   - Follow Solution 1 to change port

## After Fixing Port (If you changed to 3307)

You need to update your database connection in the app:

**File to Update:** `instagram_api\config\Database.php`

Find the line that looks like:
```php
$this->conn = new PDO("mysql:host=" . $this->host . ";dbname=" . $this->db_name, $this->username, $this->password);
```

Change to:
```php
$this->conn = new PDO("mysql:host=" . $this->host . ";port=3307;dbname=" . $this->db_name, $this->username, $this->password);
```

## Prevention Tips

1. **Always stop XAMPP properly:**
   - Don't just close the window
   - Click "Stop" for each service before closing

2. **Run XAMPP as Administrator:**
   - Right-click xampp-control.exe
   - Properties → Compatibility → Run as Administrator

3. **Backup your database regularly:**
   - Use phpMyAdmin to export your `instagram_clone` database
   - Save the SQL file regularly

## Emergency: Start Fresh Database

If nothing works and you need to start fresh:

1. Stop MySQL
2. Rename `C:\xampp\mysql\data\instagram_clone` folder to `instagram_clone_backup`
3. Start MySQL
4. Open phpMyAdmin (http://localhost/phpmyadmin)
5. Create new database: `instagram_clone`
6. Import the schema: `E:\Mobile dev Projects\i210396\database\schema.sql`

## Check If MySQL is Running

After trying fixes, verify MySQL is running:

1. Open browser
2. Go to: http://localhost/phpmyadmin
3. If it loads, MySQL is running ✓
4. If not, check the error logs

## Still Not Working?

If MySQL still won't start:

1. Check Windows Event Viewer:
   - Press Win+R → eventvwr.msc
   - Look under Windows Logs → Application
   - Find MySQL errors with details

2. Try reinstalling XAMPP:
   - Backup your database first
   - Uninstall XAMPP
   - Download fresh XAMPP from apachefriends.org
   - Reinstall and import database

---

**Most likely cause**: Port 3306 is being used by another MySQL service or Windows MySQL instance.

**Quickest fix**: Change MySQL port to 3307 in XAMPP (Solution 1)

