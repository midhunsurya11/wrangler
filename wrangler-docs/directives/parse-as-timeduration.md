# Parse as TimeDuration

The TimeDuration parser converts string representations of time durations into standardized millisecond values.

## Syntax
```
parse-as-timeduration :column_name
```

## Arguments
| Name | Description | Required? | Default |
|------|-------------|-----------|---------|
| column_name | Name of the column to parse | Yes | |

## Description
Parses string values containing time durations with units into their millisecond equivalents.

Supported units:
- ms (milliseconds)
- s (seconds) = 1000 ms
- m (minutes) = 60 s
- h (hours) = 60 m

## Examples
```
// Input: duration_col contains "1.5h"
parse-as-timeduration :duration_col
// Output: duration_col contains 5400000 (1.5 * 60 * 60 * 1000)

// Used with aggregate-stats
aggregate-stats :size :duration total_mb total_min 'MB' 'm'
```