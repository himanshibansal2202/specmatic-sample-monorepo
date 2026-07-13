package app

import "encoding/json"

func decodeJSON[T any](body []byte) (T, bool) {
	var value T
	if len(body) == 0 {
		return value, false
	}
	if err := json.Unmarshal(body, &value); err != nil {
		return value, false
	}
	return value, true
}
