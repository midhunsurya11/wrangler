# Parse as ByteSize

The ByteSize parser converts string representations of data sizes into standardized byte values.

## Syntax
```
parse-as-bytesize :column_name
```

## Arguments
| Name | Description | Required? | Default |
|------|-------------|-----------|---------|
| column_name | Name of the column to parse | Yes | |

## Description
Parses string values containing data sizes with units into their byte equivalents.

Supported units:
- B (bytes)
- KB (kilobytes) = 1024 bytes
- MB (megabytes) = 1024 KB
- GB (gigabytes) = 1024 MB
- TB (terabytes) = 1024 GB

## Examples
```
// Input: size_col contains "1.5GB"
parse-as-bytesize :size_col
// Output: size_col contains 1610612736 (1.5 * 1024 * 1024 * 1024)

// Used with aggregate-stats
aggregate-stats :file_size :response_time total_size total_time 'MB' 's'
```