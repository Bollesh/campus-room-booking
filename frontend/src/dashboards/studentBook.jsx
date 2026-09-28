import React, { useState, useEffect } from 'react';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import Header from '../components/header';

export default function StudentBook() {
    const [clubs, setClubs] = useState([]);
    const [rooms, setRooms] = useState([]);
    const [blocks, setBlocks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // Form state
    const [selectedClub, setSelectedClub] = useState('');
    const [selectedBlock, setSelectedBlock] = useState('');
    const [selectedRoom, setSelectedRoom] = useState('');
    const [startTime, setStartTime] = useState(null);
    const [endTime, setEndTime] = useState(null);
    const [purpose, setPurpose] = useState('');

    // Get student email and JWT from localStorage
    const studentEmail = localStorage.getItem('email');
    const jwt = localStorage.getItem('jwt');

    const toLocalISO = (date) => {
        const offset = date.getTimezoneOffset() * 60000;
        return new Date(date - offset).toISOString().slice(0, -1);
    };

    useEffect(() => {
        const fetchData = async () => {
        setLoading(true);
        setError('');
        try {
            // Fetch club memberships
            const clubRes = await fetch(
            `http://localhost:8080/api/memberships/student/${studentEmail}`,
            {
                headers: { Authorization: `Bearer ${jwt}` }
            }
            );
            if (!clubRes.ok) {
            throw new Error('Failed to fetch clubs');
            }
            const clubData = await clubRes.json();
            // Defensive: Only use clubName from valid ClubMembership objects
            setClubs(
            Array.isArray(clubData)
                ? clubData
                    .filter(m => m && m.clubName)
                    .map(m => m.clubName)
                : []
            );

            // Fetch all rooms
            const roomRes = await fetch(
            'http://localhost:8080/api/rooms',
            {
                headers: { Authorization: `Bearer ${jwt}` }
            }
            );
            if (!roomRes.ok) {
            throw new Error('Failed to fetch rooms');
            }
            const roomData = await roomRes.json();
            // Defensive: Only use rooms with block and room fields
            const validRooms = Array.isArray(roomData)
            ? roomData.filter(r => r && r.block && r.room)
            : [];
            setRooms(validRooms);

            // Extract unique blocks
            const blockSet = new Set(validRooms.map(r => r.block));
            setBlocks(Array.from(blockSet));

            setLoading(false);
        } catch (err) {
            setError('Failed to load data: ' + (err.message || 'Unknown error'));
            setLoading(false);
        }
        };
        fetchData();
    }, [studentEmail, jwt]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!selectedClub || !selectedBlock || !selectedRoom || !startTime || !endTime) {
        alert('Please fill all required fields');
        return;
        }
        // Format to ISO string as required by backend
        const bookingData = {
        clubName: selectedClub,
        block: selectedBlock,
        roomNo: selectedRoom,
        startTime: toLocalISO(startTime),
        endTime: toLocalISO(endTime),
        purpose: purpose,
        studentEmail: studentEmail
        };

        try {
        const res = await fetch('http://localhost:8080/api/bookings', {
            method: 'POST',
            headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${jwt}`
            },
            body: JSON.stringify(bookingData)
        });
        if (!res.ok) {
            const errData = await res.json().catch(() => ({}));
            throw new Error(errData.error || 'Booking failed');
        }
        alert('Booking created successfully!');
        setSelectedClub('');
        setSelectedBlock('');
        setSelectedRoom('');
        setStartTime(null);
        setEndTime(null);
        setPurpose('');
        } catch (err) {
        alert(err.message || 'Booking failed');
        }
    };

    if (loading) return <div>Loading...</div>;
    if (error) return <div>{error}</div>;

    return (
        <>
            <Header />
            <div className="booking-form">
            <h2>Create Room Booking</h2>
            <form onSubmit={handleSubmit}>
                <div className="form-group">
                <label>Club:</label>
                <select
                    value={selectedClub}
                    onChange={e => setSelectedClub(e.target.value)}
                    required
                >
                    <option value="">Select Club</option>
                    {clubs.map(club => (
                    <option key={club} value={club}>{club}</option>
                    ))}
                </select>
                </div>

                <div className="form-group">
                <label>Block:</label>
                <select
                    value={selectedBlock}
                    onChange={e => setSelectedBlock(e.target.value)}
                    required
                >
                    <option value="">Select Block</option>
                    {blocks.map(block => (
                    <option key={block} value={block}>{block}</option>
                    ))}
                </select>
                </div>

                <div className="form-group">
                <label>Room:</label>
                <select
                    value={selectedRoom}
                    onChange={e => setSelectedRoom(e.target.value)}
                    required
                >
                    <option value="">Select Room</option>
                    {rooms
                    .filter(r => r.block === selectedBlock)
                    .map(room => (
                        <option key={room.room} value={room.room}>
                        {room.room}
                        </option>
                    ))}
                </select>
                </div>

                <div className="form-group">
                <label>Start Time:</label>
                <DatePicker
                    selected={startTime}
                    onChange={date => setStartTime(date)}
                    showTimeSelect
                    dateFormat="MMMM d, yyyy h:mm aa"
                    minDate={new Date()}
                    required
                />
                </div>

                <div className="form-group">
                <label>End Time:    </label>
                <DatePicker
                    selected={endTime}
                    onChange={date => setEndTime(date)}
                    showTimeSelect
                    dateFormat="MMMM d, yyyy h:mm aa"
                    minDate={startTime || new Date()}
                    required
                />
                </div>

                <div className="form-group">
                <label>Purpose:</label>
                <textarea
                    value={purpose}
                    onChange={e => setPurpose(e.target.value)}
                    required
                />
                </div>

                <button type="submit">Submit Booking</button>
            </form>
            </div>
        </>
    );
}
