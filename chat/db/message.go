package db

import (
	"errors"
	"github.com/guregu/null"
	. "nkonev.name/chat/logger"
	"time"
)


type Message struct {
	Id    int64
	Text string
	ChatId int64
	OwnerId int64
	CreateDateTime time.Time
	EditDateTime null.Time
}

func (db *DB) GetMessages(chatId int64, userId int64, limit int, offset int) ([]*Message, error) {
	if rows, err := db.Query(`SELECT * FROM message WHERE chat_id IN ( SELECT chat_id FROM chat_participant WHERE user_id = $1 AND chat_id = $4 ) ORDER BY id LIMIT $2 OFFSET $3`, userId, limit, offset, chatId); err != nil {
		Logger.Errorf("Error during get chat rows %v", err)
		return nil, err
	} else {
		defer rows.Close()
		list := make([]*Message, 0)
		for rows.Next() {
			message := Message{}
			if err := rows.Scan(&message.Id, &message.Text, &message.ChatId, &message.OwnerId, &message.CreateDateTime, &message.EditDateTime); err != nil {
				Logger.Errorf("Error during scan message rows %v", err)
				return nil, err
			} else {
				list = append(list, &message)
			}
		}
		return list, nil
	}
}

func (tx *Tx) CreateMessage(m *Message) (int64, error) {
	if m == nil {
		return 0, errors.New("message required")
	} else if m.Text == "" {
		return 0, errors.New("text required")
	}

	res := tx.QueryRow(`INSERT INTO message (text, chat_id, owner_id) VALUES ($1, $2, $3) RETURNING id`, m.Text, m.ChatId, m.OwnerId)
	var id int64
	if err := res.Scan(&id); err != nil {
		Logger.Errorf("Error during getting message id %v", err)
		return 0, err
	}
	return id, nil
}

func (db *DB) CountMessages() (int64, error) {
	var count int64
	row := db.QueryRow("SELECT count(*) FROM message")
	err := row.Scan(&count)
	if err != nil {
		return 0, err
	} else {
		return count, nil
	}
}

func (db *DB) GetMessage(chatId int64, userId int64, messageId int64) (*Message, error) {
	row := db.QueryRow(`SELECT * FROM message m WHERE m.id = $1 AND chat_id in (SELECT chat_id FROM chat_participant WHERE user_id = $2 AND chat_id = $3)`, messageId, userId, chatId)
	message := Message{}
	err := row.Scan(&message.Id, &message.Text, &message.ChatId, &message.OwnerId, &message.CreateDateTime, &message.EditDateTime)
	if err != nil {
		Logger.Errorf("Error during get message row %v", err)
		return nil, err
	} else {
		return &message, nil
	}
}