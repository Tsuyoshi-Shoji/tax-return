param(
    [string]$InputDir = "$(Split-Path -Parent $MyInvocation.MyCommand.Path)",
    [string]$OutputFile = "$(Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) 'schema.mysql.sql')"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-MarkdownTableColumns {
    param(
        [string]$Content
    )

    $lines = $Content -split "`r?`n"
    $inTable = $false
    $rows = @()

    foreach ($line in $lines) {
        if ($line -match '^\|\s*No\s*\|') {
            $inTable = $true
            continue
        }

        if (-not $inTable) {
            continue
        }

        if ($line -match '^\|\s*---') {
            continue
        }

        if (-not $line.Trim().StartsWith('|')) {
            break
        }

        $parts = $line.Trim().Trim('|').Split('|') | ForEach-Object { $_.Trim() }
        if ($parts.Count -lt 7) {
            continue
        }

        if ($parts[0] -notmatch '^\d+$') {
            continue
        }

        $rows += [PSCustomObject]@{
            No = [int]$parts[0]
            PhysicalName = $parts[1]
            LogicalName = $parts[2]
            DataType = $parts[3]
            DefaultValue = $parts[4]
            Nullability = $parts[5]
            Note = $parts[6]
        }
    }

    return $rows
}

function Convert-DefaultValueToSql {
    param(
        [string]$DefaultValue
    )

    if ([string]::IsNullOrWhiteSpace($DefaultValue) -or $DefaultValue -eq '-') {
        return $null
    }

    $normalized = $DefaultValue.Trim()
    if ($normalized -match "^'(.*)'$") {
        $normalized = $matches[1]
    }

    if ($normalized -eq 'CURRENT_TIMESTAMP') {
        return 'CURRENT_TIMESTAMP'
    }

    if ($normalized -match '^(TRUE|FALSE)$') {
        return $normalized
    }

    if ($normalized -match '^-?\d+(\.\d+)?$') {
        return $normalized
    }

    # Identity values are handled by column type.
    if ($normalized -match 'auto|AUTO') {
        return $null
    }

    $escaped = $normalized.Replace("'", "''")
    return "'$escaped'"
}

function New-ConstraintName {
    param(
        [string]$Prefix,
        [string]$Table,
        [string]$Column,
        [string]$Suffix = ''
    )

    $name = "${Prefix}_${Table}_${Column}"
    if ($Suffix) {
        $name = "${name}_${Suffix}"
    }

    if ($name.Length -le 64) {
        return $name
    }

    return $name.Substring(0, 64)
}

$mdFiles = Get-ChildItem -Path $InputDir -Filter '*.md' -File |
    Where-Object { $_.Name -ne 'README.md' } |
    Sort-Object Name

if (-not $mdFiles) {
    throw "Markdown table definition files were not found in: $InputDir"
}

$sqlBlocks = @()
$generatedAt = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
$sqlBlocks += "-- Auto-generated from table-definition markdown files."
$sqlBlocks += "-- Generated at: $generatedAt"
$sqlBlocks += "-- Target dialect: MySQL 8.0"
$sqlBlocks += ""
$sqlBlocks += "SET NAMES utf8mb4;"
$sqlBlocks += ""

foreach ($file in $mdFiles) {
    $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8

    $tablePhysicalMatch = [regex]::Match($content, '(?m)^-\s+.*`([a-zA-Z0-9_]+)`\s*$')
    $tableLogicalMatch = [regex]::Match($content, '(?m)^-\s+[^`\r\n]*:\s*([^`\r\n]+)\s*$')
    $purposeMatch = [regex]::Match($content, '(?m)^##\s+.*目的\s*$\r?\n([^\r\n]+)')

    if (-not $tablePhysicalMatch.Success) {
        throw "Table physical name not found: $($file.FullName)"
    }

    $tableName = $tablePhysicalMatch.Groups[1].Value.Trim()
    $tableLogical = if ($tableLogicalMatch.Success) { $tableLogicalMatch.Groups[1].Value.Trim() } else { '' }
    $tablePurpose = if ($purposeMatch.Success) { $purposeMatch.Groups[1].Value.Trim() } else { '' }

    $columns = Get-MarkdownTableColumns -Content $content
    if (-not $columns) {
        throw "Column definitions not found: $($file.FullName)"
    }

    $columnSqlList = @()
    $pkColumns = @()
    $uniqueColumns = @()
    $fkConstraints = @()

    foreach ($col in $columns | Sort-Object No) {
        $columnName = $col.PhysicalName
        $dataType = $col.DataType
        $defaultSql = Convert-DefaultValueToSql -DefaultValue $col.DefaultValue
        $nullabilityText = ''
        if ($col.PSObject.Properties.Match('Nullability').Count -gt 0 -and $null -ne $col.Nullability) {
            $nullabilityText = ([string]$col.Nullability).Trim()
        }
        $isNullable = ($nullabilityText.Length -eq 2 -or $nullabilityText.Length -eq 4)

        $isAutoIncrement = ($columnName -eq 'id' -and $dataType -match '^BIGINT$')
        if ($isAutoIncrement) {
            $dataType = 'BIGINT AUTO_INCREMENT'
            $defaultSql = $null
        }

        $lineParts = @("  $columnName", $dataType)

        if ($isNullable) {
            $lineParts += 'NULL'
        }
        else {
            $lineParts += 'NOT NULL'
        }

        if ($defaultSql) {
            $lineParts += "DEFAULT $defaultSql"
        }

        $colCommentParts = @()
        if ($col.LogicalName) { $colCommentParts += $col.LogicalName }
        if ($col.Note -and $col.Note -ne '-') { $colCommentParts += $col.Note }
        if ($colCommentParts.Count -gt 0) {
            $comment = ($colCommentParts -join ' / ').Replace("'", "''")
            $lineParts += "COMMENT '$comment'"
        }

        $columnSqlList += ($lineParts -join ' ')

        if ($col.Note -match '(^|\s)PK(\s|$)') {
            $pkColumns += $columnName
        }

        if ($col.Note -match '(^|,\s*)UNIQUE(\s|$)') {
            $uniqueColumns += $columnName
        }

        $fkMatch = [regex]::Match($col.Note, 'FK\s*->\s*([a-zA-Z0-9_]+)\.([a-zA-Z0-9_]+)')
        if ($fkMatch.Success) {
            $fkConstraints += [PSCustomObject]@{
                ColumnName = $columnName
                RefTable = $fkMatch.Groups[1].Value
                RefColumn = $fkMatch.Groups[2].Value
            }
        }
    }

    $constraintSql = @()

    if ($pkColumns.Count -gt 0) {
        $pkColsSql = ($pkColumns | ForEach-Object { "$_" }) -join ', '
        $constraintSql += "  PRIMARY KEY ($pkColsSql)"
    }

    foreach ($uqCol in $uniqueColumns | Select-Object -Unique) {
        $uqName = New-ConstraintName -Prefix 'uq' -Table $tableName -Column $uqCol
        $constraintSql += "  CONSTRAINT $uqName UNIQUE ($uqCol)"
    }

    foreach ($fk in $fkConstraints) {
        $fkName = New-ConstraintName -Prefix 'fk' -Table $tableName -Column $fk.ColumnName -Suffix $fk.RefTable
        $constraintSql += "  CONSTRAINT $fkName FOREIGN KEY ($($fk.ColumnName)) REFERENCES $($fk.RefTable) ($($fk.RefColumn))"
    }

    $allLines = @()
    $allLines += "DROP TABLE IF EXISTS $tableName;"
    $allLines += "CREATE TABLE $tableName ("
    $allLines += ($columnSqlList + $constraintSql) -join ",`n"
    $allLines += ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"

    $tableComments = @()
    if ($tableLogical) { $tableComments += $tableLogical }
    if ($tablePurpose) { $tableComments += $tablePurpose }

    if ($tableComments.Count -gt 0) {
        $tblComment = ($tableComments -join ' / ').Replace("'", "''")
        $allLines[-1] += " COMMENT='$tblComment'"
    }

    $allLines[-1] += ';'

    $sqlBlocks += "-- Source: $($file.Name)"
    $sqlBlocks += $allLines
    $sqlBlocks += ""
}

Set-Content -Path $OutputFile -Value ($sqlBlocks -join "`n") -Encoding UTF8
Write-Host "Generated DDL: $OutputFile"

